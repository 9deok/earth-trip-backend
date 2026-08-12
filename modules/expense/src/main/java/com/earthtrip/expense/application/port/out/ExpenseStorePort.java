package com.earthtrip.expense.application.port.out;

import com.earthtrip.expense.domain.Expense;
import java.time.Instant;
import java.util.*;

public interface ExpenseStorePort {
    List<Expense> findAll(UUID trip);

    Optional<Expense> findById(UUID id);

    Expense save(Expense e);

    Optional<AdjustmentRecord> findAdjustment(UUID id);

    List<AdjustmentRecord> findAdjustments(UUID expenseId);

    AdjustmentRecord saveAdjustment(AdjustmentRecord a);

    record AdjustmentRecord(
            UUID id,
            UUID tripId,
            UUID expenseId,
            String kind,
            long amountMinor,
            String currency,
            UUID participantId,
            Map<String, Object> payload,
            UUID createdBy,
            Instant createdAt) {}
}
