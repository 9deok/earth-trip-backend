package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DataExportUseCase {

    List<ExportResult> list(UUID actorUserId);

    ExportResult get(UUID actorUserId, UUID exportId);

    ExportResult create(UUID actorUserId, UUID requestId, String format);

    record ExportResult(
        UUID exportId,
        String status,
        String format,
        UUID fileId,
        String errorCode,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt
    ) { }
}
