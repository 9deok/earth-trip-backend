package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.conflicts.by_conflict_id;

import com.earthtrip.planning.application.port.in.SyncConflictUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/conflicts/{conflictId}")
class ConflictByIdController {

    private final SyncConflictUseCase useCase;
    private final CurrentActor actor;

    ConflictByIdController(SyncConflictUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    SyncConflictUseCase.ConflictResult get(
        @PathVariable UUID tripId,
        @PathVariable UUID conflictId
    ) {
        return useCase.get(tripId, conflictId, actor.requireUserId());
    }
}
