package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.changes;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/changes")
class ChangesController {

    private final ActivityFeedUseCase useCase;
    private final CurrentActor actor;

    ChangesController(ActivityFeedUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    ActivityFeedUseCase.ChangePage get(
        @PathVariable UUID tripId,
        @RequestParam(required = false) Long after,
        @RequestParam(required = false) Integer limit
    ) {
        return useCase.changes(tripId, actor.requireUserId(), after, limit);
    }
}
