package com.earthtrip.expense.api;

import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface TripExpenseView {

    ExpenseSummary summary(UUID tripId, UUID actorUserId);

    List<Entry> searchEntries(UUID tripId, UUID actorUserId);

    record ExpenseSummary(int activeCount, int provisionalCount, List<Total> totals) { }

    record Total(String currency, long amountMinor) { }

    record Entry(
        UUID expenseId,
        String title,
        String categoryCode,
        long amountMinor,
        String currency,
        Instant occurredAt,
        String note,
        String status
    ) { }
}
