package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlements.by_settlement_id.supplements;

import com.earthtrip.expense.application.port.in.SettlementAdjustmentUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements/{settlementId}/supplements")
class SettlementSupplementsController {

    private final SettlementAdjustmentUseCase useCase;
    private final CurrentActor actor;

    SettlementSupplementsController(SettlementAdjustmentUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettlementAdjustmentUseCase.SupplementResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody SettlementSupplementRequest request) {
        return useCase.createSupplement(
                tripId,
                settlementId,
                actor.requireUserId(),
                request.requestId(),
                request.settlementBaseVersion(),
                request.minorUnitRates());
    }
}

record SettlementSupplementRequest(
        @NotNull UUID requestId,
        @PositiveOrZero long settlementBaseVersion,
        Map<String, BigDecimal> minorUnitRates) {}
