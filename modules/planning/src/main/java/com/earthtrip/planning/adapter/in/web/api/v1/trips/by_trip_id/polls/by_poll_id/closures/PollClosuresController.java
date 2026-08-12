package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.polls.by_poll_id.closures;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/polls/{pollId}/closures")
class PollClosuresController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PollClosuresController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanningResourceUseCase.ResourceResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID pollId,
            @Valid @RequestBody PollClosureMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "POLL",
                pollId,
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        pollId, null, null, null, "CLOSED", null, r.baseVersion()));
    }
}

record PollClosureMutation(@Min(0) long baseVersion) {}
