package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.place_candidates;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/place-candidates")
class PlaceCandidatesController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PlaceCandidatesController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<PlanningResourceUseCase.ResourceResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId(), "PLACE_CANDIDATE", null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningResourceUseCase.ResourceResult post(
            @PathVariable UUID tripId, @Valid @RequestBody PlaceMutation r) {
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "PLACE_CANDIDATE",
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        r.requestId(), null, null, r.payload(), "PROPOSED", r.sortOrder(), 0));
    }
}

record PlaceMutation(
        @NotNull UUID requestId, @NotNull Map<String, Object> payload, Integer sortOrder) {}
