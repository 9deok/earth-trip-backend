package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.settlements;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/settlements")
class SettlementsController {
    private final SettlementUseCase useCase;
    private final CurrentActor actor;

    SettlementsController(SettlementUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<SettlementUseCase.SettlementResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettlementUseCase.SettlementResult post(
            @PathVariable UUID tripId, @Valid @RequestBody SettlementMutation r) {
        return useCase.create(
                tripId, actor.requireUserId(), r.requestId(), r.baseCurrency(), r.minorUnitRates());
    }
}

record SettlementMutation(
        @NotNull UUID requestId,
        @NotBlank String baseCurrency,
        Map<String, @DecimalMin(value = "0", inclusive = false) BigDecimal> minorUnitRates) {}
