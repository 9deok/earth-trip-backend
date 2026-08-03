package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SyncSnapshotUseCase {

    SnapshotResult get(UUID tripId, UUID actorUserId);

    record SnapshotResult(
        UUID tripId,
        int schemaVersion,
        long cursor,
        Instant generatedAt,
        List<PlanningResourceUseCase.ResourceResult> planningResources
    ) { }
}
