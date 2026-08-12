package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlements.by_settlement_id.differences;

import com.earthtrip.expense.application.port.in.SettlementAdjustmentUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements/{settlementId}/differences")
class SettlementDifferencesController {

    private final SettlementAdjustmentUseCase useCase;
    private final CurrentActor actor;

    SettlementDifferencesController(SettlementAdjustmentUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    SettlementAdjustmentUseCase.DifferenceResult get(
            @PathVariable UUID tripId, @PathVariable UUID settlementId) {
        return useCase.differences(tripId, settlementId, actor.requireUserId());
    }
}
