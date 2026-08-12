package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.structure_changesets.by_change_set_id.reverts;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
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
@RequestMapping("/api/v1/trips/{tripId}/structure-changesets/{changeSetId}/reverts")
class StructureChangeSetRevertsController {

    private final TripStructureUseCase useCase;
    private final CurrentActor actor;

    StructureChangeSetRevertsController(TripStructureUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripStructureUseCase.ChangeSetResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID changeSetId,
            @Valid @RequestBody StructureChangeSetRevertRequest request) {
        return useCase.revert(tripId, changeSetId, actor.requireUserId(), request.baseVersion());
    }
}

record StructureChangeSetRevertRequest(@PositiveOrZero long baseVersion) {}
