package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataExportStorePort {

    List<ExportRecord> findAll(UUID userId);

    Optional<ExportRecord> findById(UUID exportId);

    ExportRecord save(ExportRecord record);

    record ExportRecord(
            UUID id,
            UUID userId,
            String status,
            String format,
            UUID fileId,
            String errorCode,
            Instant createdAt,
            Instant completedAt,
            Instant expiresAt) {}
}
