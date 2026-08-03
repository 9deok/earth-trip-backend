package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.sync_snapshot;

import com.earthtrip.planning.application.port.in.SyncSnapshotUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/sync-snapshot")
class SyncSnapshotController {

    private final SyncSnapshotUseCase useCase;
    private final CurrentActor actor;

    SyncSnapshotController(SyncSnapshotUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    SyncSnapshotUseCase.SnapshotResult get(@PathVariable UUID tripId) {
        return useCase.get(tripId, actor.requireUserId());
    }
}
