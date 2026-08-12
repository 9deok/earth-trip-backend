package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.change_cursor;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/change-cursor")
class ChangeCursorController {

    private final ActivityFeedUseCase useCase;
    private final CurrentActor actor;

    ChangeCursorController(ActivityFeedUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    ActivityFeedUseCase.ChangeCursorResult get(@PathVariable UUID tripId) {
        return useCase.latestCursor(tripId, actor.requireUserId());
    }
}
