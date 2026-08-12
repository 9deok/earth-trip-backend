package com.earthtrip.expense.application.port.in;

import java.time.Instant;
import java.util.*;

public interface ExpenseUseCase {
    List<ExpenseResult> list(UUID trip, UUID actor);

    ExpenseResult get(UUID trip, UUID id, UUID actor);

    ExpenseResult create(UUID trip, UUID actor, ExpenseCommand c);

    ExpenseResult update(UUID trip, UUID id, UUID actor, ExpenseCommand c);

    void delete(UUID trip, UUID id, UUID actor, long baseVersion);

    List<ExpenseResult> split(
            UUID trip, UUID id, UUID actor, long baseVersion, List<SplitPart> parts);

    AdjustmentResult refund(
            UUID trip,
            UUID id,
            UUID actor,
            UUID requestId,
            long amountMinor,
            UUID participantId,
            Map<String, Object> payload);

    record ExpenseCommand(
            UUID requestId,
            String title,
            String categoryCode,
            Long amountMinor,
            String currency,
            Instant occurredAt,
            Map<UUID, Long> payerContributions,
            Map<UUID, Long> participantShares,
            String visibility,
            String status,
            String note,
            long baseVersion) {}

    record SplitPart(
            UUID requestId,
            String title,
            String categoryCode,
            long amountMinor,
            Map<UUID, Long> payerContributions,
            Map<UUID, Long> participantShares) {}

    record ExpenseResult(
            UUID expenseId,
            UUID tripId,
            String title,
            String categoryCode,
            long amountMinor,
            String currency,
            Instant occurredAt,
            Map<UUID, Long> payerContributions,
            Map<UUID, Long> participantShares,
            String visibility,
            String status,
            String note,
            long version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {}

    record AdjustmentResult(
            UUID adjustmentId,
            UUID expenseId,
            String kind,
            long amountMinor,
            String currency,
            UUID participantId,
            Map<String, Object> payload,
            Instant createdAt) {}
}
