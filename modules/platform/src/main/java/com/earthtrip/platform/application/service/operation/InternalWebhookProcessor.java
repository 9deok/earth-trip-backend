package com.earthtrip.platform.application.service.operation;

import com.earthtrip.notification.api.PushDeliveryEvents;
import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.platform.application.port.out.OperationalStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class InternalWebhookProcessor {

    private final FileStorePort files;
    private final IntegrationStorePort integrations;
    private final PushDeliveryEvents pushDeliveryEvents;
    private final Clock clock;

    InternalWebhookProcessor(
            FileStorePort files,
            IntegrationStorePort integrations,
            PushDeliveryEvents pushDeliveryEvents,
            Clock clock) {
        this.files = files;
        this.integrations = integrations;
        this.pushDeliveryEvents = pushDeliveryEvents;
        this.clock = clock;
    }

    void process(String provider, OperationalStorePort.JobRecord job) {
        switch (provider) {
            case "object-storage" -> processObjectStorage(job.payload());
            case "malware-scan" -> processMalwareScan(job.payload());
            case "calendar" -> processCalendar(job.payload());
            case "push-delivery" ->
                    pushDeliveryEvents.recordDelivery(
                            requiredText(job.payload(), "deviceId"),
                            requiredText(job.payload(), "status"),
                            optionalValue(job.payload(), "providerMessageId"));
            case "financial-provider" -> processFinancialProvider(job.payload());
            default ->
                    throw EarthTripException.badRequest(
                            "UNSUPPORTED_OPERATIONAL_JOB", "지원하지 않는 운영 작업입니다.");
        }
    }

    private void processObjectStorage(Map<String, Object> payload) {
        UUID fileId = requiredUuid(payload, "fileId");
        UUID uploadSessionId = requiredUuid(payload, "uploadSessionId");
        FileStorePort.FileRecord file = loadFile(fileId);
        FileStorePort.UploadRecord upload =
                files.upload(uploadSessionId)
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "UPLOAD_SESSION_NOT_FOUND", "업로드 세션을 찾을 수 없습니다."));
        if (!upload.fileId().equals(file.id())) {
            throw EarthTripException.conflict("UPLOAD_FILE_MISMATCH", "업로드 세션과 파일이 일치하지 않습니다.");
        }
        String checksum = optionalValue(payload, "checksumSha256");
        if (checksum != null && !file.checksum().equalsIgnoreCase(checksum)) {
            throw EarthTripException.conflict(
                    "FILE_CHECKSUM_MISMATCH", "업로드 완료 이벤트의 체크섬이 파일과 일치하지 않습니다.");
        }
        Instant now = clock.instant();
        files.saveUpload(
                new FileStorePort.UploadRecord(
                        upload.id(),
                        upload.fileId(),
                        "COMPLETED",
                        upload.expiresAt(),
                        upload.createdAt(),
                        upload.abortedAt()));
        files.saveFile(copyFile(file, "SCANNING", now));
    }

    private void processMalwareScan(Map<String, Object> payload) {
        FileStorePort.FileRecord file = loadFile(requiredUuid(payload, "fileId"));
        String result = requiredText(payload, "result").toUpperCase(Locale.ROOT);
        String status =
                switch (result) {
                    case "SAFE", "CLEAN" -> "READY";
                    case "INFECTED", "MALICIOUS", "UNSAFE" -> "QUARANTINED";
                    default ->
                            throw EarthTripException.badRequest(
                                    "INVALID_MALWARE_SCAN_RESULT", "지원하지 않는 악성코드 검사 결과입니다.");
                };
        files.saveFile(copyFile(file, status, file.completedAt()));
    }

    private void processCalendar(Map<String, Object> payload) {
        IntegrationStorePort.ConnectionRecord connection =
                loadConnection(requiredUuid(payload, "connectionId"));
        String status = providerStatus(requiredText(payload, "status"));
        Instant now = clock.instant();
        integrations.saveConnection(copyConnection(connection, status, now));
        UUID tripId = optionalUuid(payload, "tripId");
        if (tripId == null) {
            return;
        }
        IntegrationStorePort.CalendarRecord calendar =
                integrations
                        .calendar(tripId)
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "CALENDAR_SYNC_NOT_FOUND",
                                                "캘린더 동기화 설정을 찾을 수 없습니다."));
        if (!calendar.connectionId().equals(connection.id())) {
            throw EarthTripException.conflict(
                    "CALENDAR_CONNECTION_MISMATCH", "캘린더 이벤트의 연결이 여행 설정과 일치하지 않습니다.");
        }
        integrations.saveCalendar(
                new IntegrationStorePort.CalendarRecord(
                        calendar.tripId(),
                        calendar.connectionId(),
                        calendar.scopeConfig(),
                        status,
                        calendar.createdBy(),
                        calendar.createdAt(),
                        now,
                        calendar.version()));
    }

    private void processFinancialProvider(Map<String, Object> payload) {
        IntegrationStorePort.ConnectionRecord connection =
                loadConnection(requiredUuid(payload, "connectionId"));
        if (!connection.kind().equals("FINANCIAL")) {
            throw EarthTripException.conflict(
                    "FINANCIAL_CONNECTION_REQUIRED", "금융 제공자 이벤트가 금융 연결을 가리키지 않습니다.");
        }
        integrations.saveConnection(
                copyConnection(
                        connection,
                        providerStatus(requiredText(payload, "status")),
                        clock.instant()));
    }

    private IntegrationStorePort.ConnectionRecord copyConnection(
            IntegrationStorePort.ConnectionRecord current, String status, Instant now) {
        String errorCode =
                status.equals("ACTIVE")
                        ? null
                        : status.equals("REAUTHORIZATION_REQUIRED")
                                ? "PROVIDER_REAUTHORIZATION_REQUIRED"
                                : null;
        return new IntegrationStorePort.ConnectionRecord(
                current.id(),
                current.userId(),
                current.kind(),
                current.provider(),
                status,
                current.scopes(),
                current.metadata(),
                current.authorizationState(),
                current.authorizationExpiresAt(),
                status.equals("ACTIVE") ? now : current.lastSuccessAt(),
                errorCode,
                current.createdAt(),
                now,
                status.equals("REVOKED") ? now : current.revokedAt(),
                current.version());
    }

    private static FileStorePort.FileRecord copyFile(
            FileStorePort.FileRecord file, String status, Instant completedAt) {
        return new FileStorePort.FileRecord(
                file.id(),
                file.ownerUserId(),
                file.fileName(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksum(),
                file.storageKey(),
                status,
                file.createdAt(),
                completedAt,
                file.deletedAt(),
                file.version());
    }

    private FileStorePort.FileRecord loadFile(UUID id) {
        return files.file(id)
                .filter(file -> file.deletedAt() == null)
                .orElseThrow(() -> EarthTripException.notFound("FILE_NOT_FOUND", "파일을 찾을 수 없습니다."));
    }

    private IntegrationStorePort.ConnectionRecord loadConnection(UUID id) {
        return integrations
                .connection(id)
                .filter(connection -> connection.revokedAt() == null)
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "INTEGRATION_CONNECTION_NOT_FOUND", "외부 연결을 찾을 수 없습니다."));
    }

    private static String providerStatus(String value) {
        String status = value.strip().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "REAUTHORIZATION_REQUIRED", "ERROR", "REVOKED").contains(status)) {
            throw EarthTripException.badRequest("INVALID_PROVIDER_STATUS", "지원하지 않는 외부 제공자 상태입니다.");
        }
        return status;
    }

    private static String requiredText(Map<String, Object> payload, String field) {
        String value = optionalValue(payload, field);
        if (value == null) {
            throw EarthTripException.badRequest(
                    "WEBHOOK_FIELD_REQUIRED", "웹훅 본문에 " + field + " 값이 필요합니다.");
        }
        return value;
    }

    private static String optionalValue(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().strip();
    }

    private static UUID requiredUuid(Map<String, Object> payload, String field) {
        UUID value = optionalUuid(payload, field);
        if (value == null) {
            throw EarthTripException.badRequest(
                    "WEBHOOK_FIELD_REQUIRED", "웹훅 본문에 " + field + " 값이 필요합니다.");
        }
        return value;
    }

    private static UUID optionalUuid(Map<String, Object> payload, String field) {
        String value = optionalValue(payload, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.badRequest("INVALID_WEBHOOK_UUID", field + " 값은 UUID여야 합니다.");
        }
    }
}
