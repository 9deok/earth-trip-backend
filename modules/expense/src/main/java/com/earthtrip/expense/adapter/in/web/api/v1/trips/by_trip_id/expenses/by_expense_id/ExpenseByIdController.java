package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expenses.by_expense_id;

import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses/{expenseId}")
class ExpenseByIdController {
    private final ExpenseUseCase useCase;
    private final CurrentActor actor;

    ExpenseByIdController(ExpenseUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    ExpenseUseCase.ExpenseResult get(@PathVariable UUID tripId, @PathVariable UUID expenseId) {
        return useCase.get(tripId, expenseId, actor.requireUserId());
    }

    @PatchMapping
    ExpenseUseCase.ExpenseResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseMutation r) {
        return useCase.update(
                tripId,
                expenseId,
                actor.requireUserId(),
                new ExpenseUseCase.ExpenseCommand(
                        expenseId,
                        r.title(),
                        r.categoryCode(),
                        r.amountMinor(),
                        r.currency(),
                        r.occurredAt(),
                        r.payerContributions(),
                        r.participantShares(),
                        r.visibility(),
                        r.status(),
                        r.note(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseDelete r) {
        useCase.delete(tripId, expenseId, actor.requireUserId(), r.baseVersion());
    }
}

record ExpenseMutation(
        String title,
        String categoryCode,
        @Positive Long amountMinor,
        String currency,
        Instant occurredAt,
        Map<UUID, @PositiveOrZero Long> payerContributions,
        Map<UUID, @PositiveOrZero Long> participantShares,
        String visibility,
        String status,
        String note,
        @Min(0) long baseVersion) {}

record ExpenseDelete(@Min(0) long baseVersion) {}
