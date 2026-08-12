package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TripExportStorePort {

    Optional<ExportRecord> find(UUID exportId);

    ExportRecord save(ExportRecord export);

    record ExportRecord(
            UUID id,
            UUID tripId,
            String format,
            Set<String> scopes,
            String status,
            String fileName,
            String mimeType,
            byte[] artifact,
            String checksumSha256,
            String failureCode,
            String failureMessage,
            int attemptCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {}
}
