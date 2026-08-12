package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidate_duplicate_queries;

import com.earthtrip.planning.application.port.in.PlanningCollectionUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidate-duplicate-queries")
class PlaceCandidateDuplicateQueriesController {

    private final PlanningCollectionUseCase useCase;
    private final CurrentActor actor;

    PlaceCandidateDuplicateQueriesController(
            PlanningCollectionUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    List<PlanningCollectionUseCase.DuplicateResult> query(
            @PathVariable UUID tripId,
            @Valid @RequestBody PlaceCandidateDuplicateQueryRequest request) {
        return useCase.placeCandidateDuplicates(tripId, actor.requireUserId(), request.command());
    }
}

record PlaceCandidateDuplicateQueryRequest(
        UUID anchorId,
        Map<String, Object> payload,
        @DecimalMin("0.0") @DecimalMax("1.0") Double minimumScore) {
    PlanningCollectionUseCase.DuplicateQuery command() {
        return new PlanningCollectionUseCase.DuplicateQuery(anchorId, payload, minimumScore);
    }
}
