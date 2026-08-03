package com.earthtrip.platform.application.service.operation;

import static com.earthtrip.platform.application.service.operation.InternalOperationResultMapper.job;
import static com.earthtrip.platform.application.service.operation.InternalOperationResultMapper.webhook;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import com.earthtrip.platform.application.port.out.OperationalStorePort;
import com.earthtrip.platform.application.port.out.WebhookSecurityPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class InternalOperationsService implements InternalOperationsUseCase {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final int MAX_WEBHOOK_BYTES = 1024 * 1024;
    private static final Set<String> JOB_STATUSES = Set.of(
        "QUEUED", "RUNNING", "SUCCEEDED", "FAILED", "CANCELLED"
    );
    private static final Set<String> DEAD_LETTER_STATUSES = Set.of("OPEN", "REPLAYED");

    private final OperationalStorePort store;
    private final WebhookSecurityPort webhookSecurity;
    private final InternalWebhookProcessor webhookProcessor;
    private final ObjectMapper json;
    private final Clock clock;

    InternalOperationsService(
        OperationalStorePort store,
        WebhookSecurityPort webhookSecurity,
        InternalWebhookProcessor webhookProcessor,
        ObjectMapper json,
        Clock clock
    ) {
        this.store = store;
        this.webhookSecurity = webhookSecurity;
        this.webhookProcessor = webhookProcessor;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public WebhookResult acceptWebhook(
        String provider,
        String eventId,
        String timestamp,
        String signature,
        String rawBody
    ) {
        String body = rawBody == null ? "" : rawBody;
        if (body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_WEBHOOK_BYTES) {
            throw new EarthTripException(
                "WEBHOOK_PAYLOAD_TOO_LARGE",
                413,
                "웹훅 본문은 1MB를 초과할 수 없습니다."
            );
        }
        WebhookSecurityPort.VerifiedWebhook verified = webhookSecurity.verify(
            provider,
            eventId,
            timestamp,
            signature,
            body
        );
        OperationalStorePort.WebhookReceiptRecord existing = store.webhookReceipt(
            verified.provider(),
            verified.eventId()
        ).orElse(null);
        if (existing != null) {
            if (!existing.payloadDigest().equals(verified.payloadDigest())) {
                throw EarthTripException.conflict(
                    "WEBHOOK_EVENT_ID_REUSED",
                    "같은 이벤트 ID가 다른 본문과 함께 다시 사용되었습니다."
                );
            }
            OperationalStorePort.JobRecord job = loadJob(existing.jobId());
            return webhook(job, existing, true);
        }

        Instant now = clock.instant();
        UUID jobId = UUID.randomUUID();
        OperationalStorePort.JobRecord queued = store.saveJob(new OperationalStorePort.JobRecord(
            jobId,
            jobType(verified.provider()),
            verified.eventId(),
            "QUEUED",
            readBody(body),
            1,
            now,
            now,
            now,
            null,
            null,
            null,
            0
        ));
        OperationalStorePort.WebhookReceiptRecord receipt = store.saveWebhookReceipt(
            new OperationalStorePort.WebhookReceiptRecord(
                UUID.randomUUID(),
                verified.provider(),
                verified.eventId(),
                verified.payloadDigest(),
                queued.id(),
                now
            )
        );
        audit(
            "PROVIDER",
            verified.provider(),
            "WEBHOOK_RECEIVED",
            "OPERATIONAL_JOB",
            jobId.toString(),
            "ACCEPTED",
            Map.of("sourceEventId", verified.eventId())
        );
        OperationalStorePort.JobRecord processed = process(queued, verified.provider());
        return webhook(processed, receipt, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResult> jobs(String status, String jobType, int limit) {
        String normalizedStatus = optionalEnum(status, JOB_STATUSES, "INVALID_JOB_STATUS");
        String normalizedType = optionalText(jobType, 50, "INVALID_JOB_TYPE");
        return store.jobs(normalizedStatus, normalizedType, validateLimit(limit)).stream()
            .map(InternalOperationResultMapper::job)
            .toList();
    }

    @Override
    public JobResult retryJob(UUID jobId) {
        OperationalStorePort.JobRecord current = loadJob(jobId);
        if (current.status().equals("SUCCEEDED")) {
            throw EarthTripException.conflict(
                "OPERATIONAL_JOB_ALREADY_SUCCEEDED",
                "이미 성공한 운영 작업은 재시도할 수 없습니다."
            );
        }
        if (current.status().equals("RUNNING")) {
            throw EarthTripException.conflict(
                "OPERATIONAL_JOB_RUNNING",
                "실행 중인 운영 작업은 재시도할 수 없습니다."
            );
        }
        Instant now = clock.instant();
        OperationalStorePort.JobRecord queued = store.saveJob(new OperationalStorePort.JobRecord(
            current.id(),
            current.jobType(),
            current.sourceEventId(),
            "QUEUED",
            current.payload(),
            current.attemptCount() + 1,
            now,
            current.createdAt(),
            now,
            null,
            null,
            null,
            current.version()
        ));
        audit(
            "ADMIN",
            null,
            "OPERATIONAL_JOB_RETRY_REQUESTED",
            "OPERATIONAL_JOB",
            jobId.toString(),
            "ACCEPTED",
            Map.of("attemptCount", queued.attemptCount())
        );
        return job(process(queued, providerFromJobType(queued.jobType())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeadLetterResult> deadLetters(String status, int limit) {
        String normalized = optionalEnum(
            status,
            DEAD_LETTER_STATUSES,
            "INVALID_DEAD_LETTER_STATUS"
        );
        return store.deadLetters(normalized, validateLimit(limit)).stream()
            .map(InternalOperationResultMapper::deadLetter)
            .toList();
    }

    @Override
    public JobResult replayDeadLetter(UUID eventId) {
        OperationalStorePort.DeadLetterRecord event = store.deadLetter(eventId)
            .orElseThrow(() -> EarthTripException.notFound(
                "DEAD_LETTER_NOT_FOUND",
                "데드레터 이벤트를 찾을 수 없습니다."
            ));
        if (!event.status().equals("OPEN")) {
            throw EarthTripException.conflict(
                "DEAD_LETTER_ALREADY_REPLAYED",
                "이미 재처리된 데드레터 이벤트입니다."
            );
        }
        JobResult retried = retryJob(event.jobId());
        if (retried.status().equals("SUCCEEDED")) {
            store.saveDeadLetter(new OperationalStorePort.DeadLetterRecord(
                event.id(),
                event.jobId(),
                event.eventType(),
                event.payload(),
                event.errorCode(),
                event.errorMessage(),
                "REPLAYED",
                event.createdAt(),
                clock.instant(),
                event.version()
            ));
            audit(
                "ADMIN",
                null,
                "DEAD_LETTER_REPLAYED",
                "DEAD_LETTER",
                event.id().toString(),
                "SUCCEEDED",
                Map.of("jobId", event.jobId().toString())
            );
        }
        return retried;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditResult> auditEvents(
        String action,
        String targetType,
        String targetId,
        int limit
    ) {
        return store.auditEvents(
            optionalText(action, 80, "INVALID_AUDIT_ACTION"),
            optionalText(targetType, 50, "INVALID_AUDIT_TARGET_TYPE"),
            optionalText(targetId, 160, "INVALID_AUDIT_TARGET_ID"),
            validateLimit(limit)
        ).stream().map(InternalOperationResultMapper::audit).toList();
    }

    private OperationalStorePort.JobRecord process(
        OperationalStorePort.JobRecord queued,
        String provider
    ) {
        Instant startedAt = clock.instant();
        OperationalStorePort.JobRecord running = store.saveJob(copyJob(
            queued,
            "RUNNING",
            startedAt,
            null,
            null,
            null
        ));
        try {
            webhookProcessor.process(provider, running);
            Instant completedAt = clock.instant();
            OperationalStorePort.JobRecord succeeded = store.saveJob(copyJob(
                running,
                "SUCCEEDED",
                completedAt,
                completedAt,
                null,
                null
            ));
            audit(
                "PROVIDER",
                provider,
                "WEBHOOK_PROCESSED",
                "OPERATIONAL_JOB",
                succeeded.id().toString(),
                "SUCCEEDED",
                Map.of("jobType", succeeded.jobType())
            );
            return succeeded;
        } catch (RuntimeException exception) {
            String code = exception instanceof EarthTripException earthTrip
                ? earthTrip.code()
                : "INTERNAL_WEBHOOK_PROCESSING_FAILED";
            String message = clip(exception.getMessage(), 500);
            Instant failedAt = clock.instant();
            OperationalStorePort.JobRecord failed = store.saveJob(copyJob(
                running,
                "FAILED",
                failedAt,
                failedAt,
                code,
                message
            ));
            OperationalStorePort.DeadLetterRecord old = store.openDeadLetterForJob(failed.id())
                .orElse(null);
            store.saveDeadLetter(new OperationalStorePort.DeadLetterRecord(
                old == null ? UUID.randomUUID() : old.id(),
                failed.id(),
                failed.jobType(),
                failed.payload(),
                code,
                message,
                "OPEN",
                old == null ? failedAt : old.createdAt(),
                null,
                old == null ? 0 : old.version()
            ));
            audit(
                "PROVIDER",
                provider,
                "WEBHOOK_PROCESSING_FAILED",
                "OPERATIONAL_JOB",
                failed.id().toString(),
                "FAILED",
                Map.of("errorCode", code)
            );
            return failed;
        }
    }

    private OperationalStorePort.JobRecord copyJob(
        OperationalStorePort.JobRecord current,
        String status,
        Instant updatedAt,
        Instant completedAt,
        String errorCode,
        String errorMessage
    ) {
        return new OperationalStorePort.JobRecord(
            current.id(),
            current.jobType(),
            current.sourceEventId(),
            status,
            current.payload(),
            current.attemptCount(),
            current.availableAt(),
            current.createdAt(),
            updatedAt,
            completedAt,
            errorCode,
            errorMessage,
            current.version()
        );
    }

    private OperationalStorePort.JobRecord loadJob(UUID id) {
        return store.job(id).orElseThrow(() -> EarthTripException.notFound(
            "OPERATIONAL_JOB_NOT_FOUND",
            "운영 작업을 찾을 수 없습니다."
        ));
    }

    private void audit(
        String actorType,
        String actorId,
        String action,
        String targetType,
        String targetId,
        String outcome,
        Map<String, Object> metadata
    ) {
        store.saveAudit(new OperationalStorePort.AuditRecord(
            null,
            UUID.randomUUID(),
            actorType,
            actorId,
            action,
            targetType,
            targetId,
            outcome,
            metadata,
            clock.instant()
        ));
    }

    private Map<String, Object> readBody(String rawBody) {
        if (rawBody.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = json.readValue(rawBody, MAP);
            if (parsed == null) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
        } catch (JsonProcessingException exception) {
            throw EarthTripException.badRequest(
                "INVALID_WEBHOOK_BODY",
                "웹훅 본문은 JSON 객체여야 합니다."
            );
        }
    }

    private static String optionalEnum(String value, Set<String> allowed, String code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw EarthTripException.badRequest(code, "지원하지 않는 상태 필터입니다.");
        }
        return normalized;
    }

    private static String optionalText(String value, int maxLength, String code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw EarthTripException.badRequest(code, "검색 필터 값이 너무 깁니다.");
        }
        return normalized;
    }

    private static int validateLimit(int limit) {
        if (limit < 1 || limit > 200) {
            throw EarthTripException.badRequest(
                "INVALID_PAGE_LIMIT",
                "limit은 1에서 200 사이여야 합니다."
            );
        }
        return limit;
    }

    private static String jobType(String provider) {
        return provider.toUpperCase(Locale.ROOT).replace('-', '_') + "_WEBHOOK";
    }

    private static String providerFromJobType(String jobType) {
        if (!jobType.endsWith("_WEBHOOK")) {
            throw EarthTripException.badRequest(
                "UNSUPPORTED_OPERATIONAL_JOB",
                "웹훅으로 생성되지 않은 작업은 이 처리기로 재시도할 수 없습니다."
            );
        }
        return jobType.substring(0, jobType.length() - "_WEBHOOK".length())
            .toLowerCase(Locale.ROOT)
            .replace('_', '-');
    }

    private static String clip(String value, int maxLength) {
        String normalized = value == null || value.isBlank()
            ? "알 수 없는 처리 오류"
            : value.strip();
        return normalized.length() <= maxLength
            ? normalized
            : normalized.substring(0, maxLength);
    }

}
