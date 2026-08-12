package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface TripExportUseCase {

    ExportResult create(UUID tripId, UUID actorUserId, ExportCommand command);

    ExportResult get(UUID tripId, UUID exportId, UUID actorUserId);

    ExportResult retry(UUID tripId, UUID exportId, UUID actorUserId, long baseVersion);

    ExportResult cancel(UUID tripId, UUID exportId, UUID actorUserId, long baseVersion);

    ArtifactResult artifact(UUID tripId, UUID exportId, UUID actorUserId);

    record ExportCommand(UUID requestId, String format, Set<String> scopes) {}

    record ExportResult(
            UUID exportId,
            UUID tripId,
            String format,
            Set<String> scopes,
            String status,
            String fileName,
            String mimeType,
            Long sizeBytes,
            String checksumSha256,
            String downloadPath,
            String failureCode,
            String failureMessage,
            int attemptCount,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    record ArtifactResult(String fileName, String mimeType, byte[] content) {}
}
