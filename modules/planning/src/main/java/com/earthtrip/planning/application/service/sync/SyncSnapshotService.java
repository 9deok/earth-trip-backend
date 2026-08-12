package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.SyncSnapshotUseCase;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SyncSnapshotService implements SyncSnapshotUseCase {

    private static final int SCHEMA_VERSION = 1;

    private final PlanningResourceUseCase resources;
    private final ActivityOperationStorePort activities;
    private final Clock clock;

    SyncSnapshotService(
            PlanningResourceUseCase resources, ActivityOperationStorePort activities, Clock clock) {
        this.resources = resources;
        this.activities = activities;
        this.clock = clock;
    }

    @Override
    public SnapshotResult get(UUID tripId, UUID actorUserId) {
        return new SnapshotResult(
                tripId,
                SCHEMA_VERSION,
                activities.latestSequence(tripId),
                clock.instant(),
                resources.listAll(tripId, actorUserId));
    }
}
