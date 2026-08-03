package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidate_merges.by_merge_id.reverts;

import com.earthtrip.planning.application.port.in.PlanningMergeUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidate-merges/{mergeId}/reverts")
class PlaceCandidateMergeRevertsController {

    private final PlanningMergeUseCase useCase;
    private final CurrentActor actor;

    PlaceCandidateMergeRevertsController(PlanningMergeUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    PlanningMergeUseCase.MergeResult revert(
        @PathVariable UUID tripId,
        @PathVariable UUID mergeId
    ) {
        return useCase.revertPlaceCandidateMerge(
            tripId, mergeId, actor.requireUserId()
        );
    }
}
