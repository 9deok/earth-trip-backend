package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates.by_candidate_id.comments;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidates/{candidateId}/comments")
class PlaceCommentsController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PlaceCommentsController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<PlanningResourceUseCase.ResourceResult> get(
            @PathVariable UUID tripId, @PathVariable UUID candidateId) {
        return useCase.list(tripId, actor.requireUserId(), "PLACE_COMMENT", candidateId, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningResourceUseCase.ResourceResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody CommentMutation r) {
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "PLACE_COMMENT",
                PlanningResourceUseCase.WritePermission.MEMBER,
                new PlanningResourceUseCase.ResourceCommand(
                        r.requestId(), candidateId, null, r.payload(), "ACTIVE", 0, 0));
    }
}

record CommentMutation(@NotNull UUID requestId, @NotNull Map<String, Object> payload) {}
