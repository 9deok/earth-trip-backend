package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.decisions.by_decision_id;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/decisions/{decisionId}")
class DecisionByIdController {
    private final PlanningResourceUseCase useCase;
    private final CurrentActor actor;

    DecisionByIdController(PlanningResourceUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    PlanningResourceUseCase.ResourceResult get(
            @PathVariable UUID tripId, @PathVariable UUID decisionId) {
        return useCase.get(tripId, actor.requireUserId(), "DECISION", decisionId);
    }
}
