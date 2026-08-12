package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.cash_movements;

import com.earthtrip.expense.application.port.in.FinanceLedgerUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/cash-movements")
class CashMovementsController {
    private final FinanceLedgerUseCase useCase;
    private final CurrentActor actor;

    CashMovementsController(FinanceLedgerUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<FinanceLedgerUseCase.CashResult> get(@PathVariable UUID tripId) {
        return useCase.cash(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FinanceLedgerUseCase.CashResult post(
            @PathVariable UUID tripId, @Valid @RequestBody CashMutation r) {
        return useCase.createCash(
                tripId,
                actor.requireUserId(),
                new FinanceLedgerUseCase.CashCommand(
                        r.requestId(),
                        r.movementType(),
                        r.amountMinor(),
                        r.currency(),
                        r.payload(),
                        r.status(),
                        r.occurredAt(),
                        0));
    }
}

record CashMutation(
        @NotNull UUID requestId,
        @NotBlank String movementType,
        long amountMinor,
        @NotBlank String currency,
        Map<String, Object> payload,
        String status,
        @NotNull Instant occurredAt) {}
