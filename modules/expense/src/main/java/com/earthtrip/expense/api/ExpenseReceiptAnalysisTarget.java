package com.earthtrip.expense.api;

import java.util.Map;
import java.util.UUID;

public interface ExpenseReceiptAnalysisTarget {

    TargetResult get(UUID tripId, UUID expenseId, UUID actorUserId);

    TargetResult confirm(
        UUID tripId,
        UUID expenseId,
        UUID actorUserId,
        Map<String, Object> confirmedFields,
        long baseVersion
    );

    record TargetResult(UUID targetId, Map<String, Object> fields, long version) { }
}
