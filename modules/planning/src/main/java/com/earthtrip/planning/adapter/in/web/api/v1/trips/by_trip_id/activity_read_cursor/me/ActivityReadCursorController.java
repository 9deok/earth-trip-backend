package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.activity_read_cursor.me;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/activity-read-cursor/me")
class ActivityReadCursorController {

    private final ActivityFeedUseCase useCase;
    private final CurrentActor actor;

    ActivityReadCursorController(ActivityFeedUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    ActivityFeedUseCase.ReadCursorResult put(
        @PathVariable UUID tripId,
        @Valid @RequestBody ActivityReadCursorRequest request
    ) {
        return useCase.updateReadCursor(
            tripId, actor.requireUserId(), request.sequenceId()
        );
    }
}

record ActivityReadCursorRequest(@PositiveOrZero long sequenceId) { }
