package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expenses;

import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses")
class ExpensesController {
    private final ExpenseUseCase useCase;
    private final CurrentActor actor;

    ExpensesController(ExpenseUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<ExpenseUseCase.ExpenseResult> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseUseCase.ExpenseResult post(
            @PathVariable UUID tripId, @Valid @RequestBody ExpenseMutation r) {
        return useCase.create(tripId, actor.requireUserId(), command(r, 0));
    }

    static ExpenseUseCase.ExpenseCommand command(ExpenseMutation r, long v) {
        return new ExpenseUseCase.ExpenseCommand(
                r.requestId(),
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
                v);
    }
}

record ExpenseMutation(
        @NotNull UUID requestId,
        @NotBlank String title,
        @NotBlank String categoryCode,
        @NotNull @Positive Long amountMinor,
        @NotBlank String currency,
        @NotNull Instant occurredAt,
        @NotEmpty Map<UUID, @PositiveOrZero Long> payerContributions,
        @NotEmpty Map<UUID, @PositiveOrZero Long> participantShares,
        String visibility,
        String status,
        String note) {}
