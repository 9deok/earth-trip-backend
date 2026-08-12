package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.activities.by_activity_id.reverts;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/activities/{activityId}/reverts")
class ActivityRevertsController {

    private final ActivityFeedUseCase useCase;
    private final CurrentActor actor;

    ActivityRevertsController(ActivityFeedUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ActivityFeedUseCase.RevertResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityRevertRequest request) {
        return useCase.revert(
                tripId, activityId, actor.requireUserId(), request.resourceBaseVersion());
    }
}

record ActivityRevertRequest(@PositiveOrZero long resourceBaseVersion) {}
