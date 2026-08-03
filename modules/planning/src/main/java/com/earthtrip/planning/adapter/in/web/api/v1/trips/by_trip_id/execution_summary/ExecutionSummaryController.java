package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.execution_summary;

import com.earthtrip.planning.application.port.in.ExecutionSummaryUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/execution-summary")
class ExecutionSummaryController {

    private final ExecutionSummaryUseCase useCase;
    private final CurrentActor actor;

    ExecutionSummaryController(ExecutionSummaryUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    ExecutionSummaryUseCase.SummaryResult get(@PathVariable UUID tripId) {
        return useCase.get(tripId, actor.requireUserId());
    }
}
