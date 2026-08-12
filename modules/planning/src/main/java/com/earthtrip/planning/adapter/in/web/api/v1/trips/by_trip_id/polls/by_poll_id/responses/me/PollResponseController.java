package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.polls.by_poll_id.responses.me;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/polls/{pollId}/responses/me")
class PollResponseController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    PollResponseController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PutMapping
    PlanningResourceUseCase.UserStateResult put(
            @PathVariable UUID tripId,
            @PathVariable UUID pollId,
            @Valid @RequestBody PollResponseMutation r) {
        return useCase.putUserState(
                tripId, actor.requireUserId(), "POLL", pollId, "RESPONSE", r.value());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID tripId, @PathVariable UUID pollId) {
        useCase.deleteUserState(tripId, actor.requireUserId(), "POLL", pollId, "RESPONSE");
    }
}

record PollResponseMutation(@NotNull Map<String, Object> value) {}
