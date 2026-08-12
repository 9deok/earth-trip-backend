package com.earthtrip.expense.adapter.in.web.api.v1.trips.by_trip_id.expenses.by_expense_id.refunds;

import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/expenses/{expenseId}/refunds")
class ExpenseRefundsController {
    private final ExpenseUseCase useCase;
    private final CurrentActor actor;

    ExpenseRefundsController(ExpenseUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseUseCase.AdjustmentResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody RefundMutation r) {
        return useCase.refund(
                tripId,
                expenseId,
                actor.requireUserId(),
                r.requestId(),
                r.amountMinor(),
                r.participantId(),
                r.payload());
    }
}

record RefundMutation(
        @NotNull UUID requestId,
        @Positive long amountMinor,
        UUID participantId,
        Map<String, Object> payload) {}
