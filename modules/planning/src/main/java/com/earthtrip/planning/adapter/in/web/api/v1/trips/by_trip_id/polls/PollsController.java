package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.polls;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/polls")
class PollsController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PollsController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<PlanningResourceUseCase.ResourceResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId(), "POLL", null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningResourceUseCase.ResourceResult post(
            @PathVariable UUID tripId, @Valid @RequestBody PollMutation r) {
        return useCase.create(
                tripId,
                actor.requireUserId(),
                "POLL",
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        r.requestId(), null, null, r.payload(), "OPEN", 0, 0));
    }
}

record PollMutation(@NotNull UUID requestId, @NotNull Map<String, Object> payload) {}
