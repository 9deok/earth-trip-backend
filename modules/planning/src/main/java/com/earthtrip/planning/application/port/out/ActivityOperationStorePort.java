package com.earthtrip.planning.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ActivityOperationStorePort {

    List<ActivityRecord> activities(UUID tripId, long after, int limit);

    Optional<ActivityRecord> findActivity(UUID activityId);

    long latestSequence(UUID tripId);

    void appendActivity(
        UUID tripId,
        UUID actorId,
        String action,
        String resourceType,
        UUID resourceId,
        Map<String, Object> details,
        Instant occurredAt
    );

    Optional<OperationRecord> findOperation(UUID operationId);

    OperationRecord saveOperation(OperationRecord record);

    record ActivityRecord(
        long sequenceId,
        UUID activityId,
        UUID tripId,
        UUID actorId,
        String action,
        String resourceType,
        UUID resourceId,
        Map<String, Object> details,
        Instant occurredAt
    ) { }

    record OperationRecord(
        UUID operationId,
        UUID tripId,
        UUID actorId,
        String status,
        String resourceType,
        UUID resourceId,
        Map<String, Object> result,
        Instant createdAt
    ) { }
}
