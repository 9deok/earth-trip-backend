package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SyncConflictUseCase {

    List<ConflictResult> list(UUID tripId, UUID actorUserId);

    ConflictResult get(UUID tripId, UUID conflictId, UUID actorUserId);

    ConflictResult resolve(
        UUID tripId,
        UUID conflictId,
        UUID actorUserId,
        ResolutionCommand command
    );

    record ResolutionCommand(String strategy, Map<String, Object> mergedPayload, long baseVersion) { }

    record ConflictResult(
        UUID conflictId,
        UUID operationId,
        String action,
        String resourceType,
        UUID resourceId,
        Map<String, Object> deviceCommand,
        Map<String, Object> serverSnapshot,
        List<String> mergeableFields,
        String status,
        String resolution,
        Instant createdAt,
        Instant resolvedAt,
        long version
    ) { }
}
