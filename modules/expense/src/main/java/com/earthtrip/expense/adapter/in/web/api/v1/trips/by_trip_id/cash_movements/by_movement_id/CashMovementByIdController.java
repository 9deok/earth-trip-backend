package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.cash_movements.by_movement_id;

import com.earthtrip.expense.application.port.in.FinanceLedgerUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/cash-movements/{movementId}")
class CashMovementByIdController {
    private final FinanceLedgerUseCase useCase;
    private final CurrentActor actor;

    CashMovementByIdController(FinanceLedgerUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PatchMapping
    FinanceLedgerUseCase.CashResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID movementId,
            @Valid @RequestBody CashMutation r) {
        return useCase.updateCash(
                tripId,
                movementId,
                actor.requireUserId(),
                new FinanceLedgerUseCase.CashCommand(
                        movementId,
                        r.movementType(),
                        r.amountMinor(),
                        r.currency(),
                        r.payload(),
                        r.status(),
                        r.occurredAt(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID movementId,
            @Valid @RequestBody CashDelete r) {
        useCase.deleteCash(tripId, movementId, actor.requireUserId(), r.baseVersion());
    }
}

record CashMutation(
        String movementType,
        long amountMinor,
        String currency,
        Map<String, Object> payload,
        String status,
        Instant occurredAt,
        @Min(0) long baseVersion) {}

record CashDelete(@Min(0) long baseVersion) {}
