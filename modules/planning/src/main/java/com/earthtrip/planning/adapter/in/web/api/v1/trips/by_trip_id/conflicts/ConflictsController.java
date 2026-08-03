package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.conflicts;

import com.earthtrip.planning.application.port.in.SyncConflictUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/conflicts")
class ConflictsController {

    private final SyncConflictUseCase useCase;
    private final CurrentActor actor;

    ConflictsController(SyncConflictUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<SyncConflictUseCase.ConflictResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }
}
