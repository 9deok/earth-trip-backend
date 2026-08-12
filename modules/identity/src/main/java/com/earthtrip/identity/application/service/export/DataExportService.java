package com.earthtrip.identity.application.service.export;

import com.earthtrip.identity.application.port.in.DataExportUseCase;
import com.earthtrip.identity.application.port.out.DataExportStorePort;
import com.earthtrip.identity.application.port.out.PersonalDataExporterPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DataExportService implements DataExportUseCase {

    private static final Set<String> FORMATS = Set.of("JSON", "ZIP");

    private final DataExportStorePort store;
    private final PersonalDataExporterPort exporter;
    private final Clock clock;

    DataExportService(DataExportStorePort store, PersonalDataExporterPort exporter, Clock clock) {
        this.store = store;
        this.exporter = exporter;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportResult> list(UUID actorUserId) {
        return store.findAll(actorUserId).stream().map(DataExportService::result).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResult get(UUID actorUserId, UUID exportId) {
        return result(
                store.findById(exportId)
                        .filter(record -> record.userId().equals(actorUserId))
                        .orElseThrow(DataExportService::notFound));
    }

    @Override
    public ExportResult create(UUID actorUserId, UUID requestId, String format) {
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "요청 ID가 필요합니다.");
        }
        String safeFormat = format(format);
        DataExportStorePort.ExportRecord existing = store.findById(requestId).orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(actorUserId) || !existing.format().equals(safeFormat)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 개인정보 내보내기에 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        Instant now = clock.instant();
        DataExportStorePort.ExportRecord processing =
                store.save(
                        new DataExportStorePort.ExportRecord(
                                requestId,
                                actorUserId,
                                "PROCESSING",
                                safeFormat,
                                null,
                                null,
                                now,
                                null,
                                null));
        try {
            PersonalDataExporterPort.ExportArtifact artifact =
                    exporter.export(actorUserId, requestId, safeFormat);
            return result(
                    store.save(
                            new DataExportStorePort.ExportRecord(
                                    processing.id(),
                                    processing.userId(),
                                    "READY",
                                    processing.format(),
                                    artifact.fileId(),
                                    null,
                                    processing.createdAt(),
                                    clock.instant(),
                                    artifact.expiresAt())));
        } catch (EarthTripException exception) {
            return result(
                    store.save(
                            new DataExportStorePort.ExportRecord(
                                    processing.id(),
                                    processing.userId(),
                                    "FAILED",
                                    processing.format(),
                                    null,
                                    exception.code(),
                                    processing.createdAt(),
                                    clock.instant(),
                                    null)));
        }
    }

    private static String format(String value) {
        String normalized = value == null ? "JSON" : value.strip().toUpperCase(Locale.ROOT);
        if (!FORMATS.contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_DATA_EXPORT_FORMAT", "개인정보 내보내기 형식은 JSON 또는 ZIP이어야 합니다.");
        }
        return normalized;
    }

    private static ExportResult result(DataExportStorePort.ExportRecord record) {
        return new ExportResult(
                record.id(),
                record.status(),
                record.format(),
                record.fileId(),
                record.errorCode(),
                record.createdAt(),
                record.completedAt(),
                record.expiresAt());
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("DATA_EXPORT_NOT_FOUND", "개인정보 내보내기 작업을 찾을 수 없습니다.");
    }
}
