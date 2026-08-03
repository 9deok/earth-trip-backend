package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.changesets.by_change_set_id.reverts;

import com.earthtrip.planning.application.port.in.DayChangeSetUseCase;
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
@RequestMapping(
    "/api/v1/trips/{tripId}/days/{dayId}/changesets/{changeSetId}/reverts"
)
class DayChangeSetRevertsController {

    private final DayChangeSetUseCase useCase;
    private final CurrentActor actor;

    DayChangeSetRevertsController(DayChangeSetUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DayChangeSetUseCase.ChangeSetResult post(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @PathVariable UUID changeSetId,
        @Valid @RequestBody DayChangeSetRevertRequest request
    ) {
        return useCase.revert(
            tripId, dayId, changeSetId, actor.requireUserId(), request.baseVersion()
        );
    }
}

record DayChangeSetRevertRequest(@PositiveOrZero long baseVersion) { }
