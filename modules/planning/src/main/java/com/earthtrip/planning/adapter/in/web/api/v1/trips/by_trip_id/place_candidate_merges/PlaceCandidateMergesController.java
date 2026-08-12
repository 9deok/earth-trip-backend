package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidate_merges;

import com.earthtrip.planning.application.port.in.PlanningMergeUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidate-merges")
class PlaceCandidateMergesController {

    private final PlanningMergeUseCase useCase;
    private final CurrentActor actor;

    PlaceCandidateMergesController(PlanningMergeUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningMergeUseCase.MergeResult merge(
            @PathVariable UUID tripId, @Valid @RequestBody PlaceCandidateMergeRequest request) {
        return useCase.mergePlaceCandidates(tripId, actor.requireUserId(), request.command());
    }
}

record PlaceCandidateMergeRequest(
        @NotNull UUID requestId,
        @NotNull UUID primaryId,
        @NotEmpty List<@NotNull UUID> duplicateIds,
        Map<String, Object> mergedPayload,
        @PositiveOrZero long primaryBaseVersion,
        @NotNull Map<UUID, @PositiveOrZero Long> duplicateBaseVersions) {
    PlanningMergeUseCase.MergeCommand command() {
        return new PlanningMergeUseCase.MergeCommand(
                requestId,
                primaryId,
                duplicateIds,
                mergedPayload,
                primaryBaseVersion,
                duplicateBaseVersions);
    }
}
