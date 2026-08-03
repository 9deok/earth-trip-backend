package com.earthtrip.planning.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SyncStateStorePort {

    long readCursor(UUID tripId, UUID userId);

    ReadCursorRecord saveReadCursor(ReadCursorRecord record);

    List<ConflictRecord> findOpenConflicts(UUID tripId);

    Optional<ConflictRecord> findConflict(UUID conflictId);

    ConflictRecord saveConflict(ConflictRecord record);

    record ReadCursorRecord(
        UUID tripId,
        UUID userId,
        long sequenceId,
        Instant updatedAt
    ) { }

    record ConflictRecord(
        UUID conflictId,
        UUID operationId,
        UUID tripId,
        UUID actorId,
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
