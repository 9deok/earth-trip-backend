package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlements.by_settlement_id.recalculation_previews;

import com.earthtrip.expense.application.port.in.SettlementAdjustmentUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements/{settlementId}/recalculation-previews")
class SettlementRecalculationPreviewsController {

    private final SettlementAdjustmentUseCase useCase;
    private final CurrentActor actor;

    SettlementRecalculationPreviewsController(
            SettlementAdjustmentUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    SettlementAdjustmentUseCase.DifferenceResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID settlementId,
            @RequestBody(required = false) SettlementRecalculationRequest request) {
        return useCase.recalculate(
                tripId,
                settlementId,
                actor.requireUserId(),
                request == null ? null : request.minorUnitRates());
    }
}

record SettlementRecalculationRequest(Map<String, BigDecimal> minorUnitRates) {}
