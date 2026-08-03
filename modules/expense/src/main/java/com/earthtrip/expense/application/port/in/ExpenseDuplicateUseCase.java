package com.earthtrip.expense.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExpenseDuplicateUseCase {

    List<DuplicateResult> query(UUID tripId, UUID actorUserId, DuplicateQuery query);

    record DuplicateQuery(
        UUID sourceId,
        String title,
        long amountMinor,
        String currency,
        Instant occurredAt
    ) { }

    record DuplicateResult(
        UUID expenseId,
        double score,
        List<String> reasons,
        ExpenseUseCase.ExpenseResult expense
    ) { }
}
