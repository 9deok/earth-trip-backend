package com.earthtrip.expense.application.service.view;

import com.earthtrip.expense.api.ExpenseReceiptAnalysisTarget;
import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ExpenseReceiptAnalysisTargetService implements ExpenseReceiptAnalysisTarget {

    private final ExpenseUseCase expenses;

    ExpenseReceiptAnalysisTargetService(ExpenseUseCase expenses) {
        this.expenses = expenses;
    }

    @Override
    @Transactional(readOnly = true)
    public TargetResult get(UUID tripId, UUID expenseId, UUID actorUserId) {
        return result(expenses.get(tripId, expenseId, actorUserId));
    }

    @Override
    public TargetResult confirm(
        UUID tripId,
        UUID expenseId,
        UUID actorUserId,
        Map<String, Object> fields,
        long baseVersion
    ) {
        ExpenseUseCase.ExpenseResult current = expenses.get(tripId, expenseId, actorUserId);
        Map<String, Object> value = fields == null ? Map.of() : fields;
        Long amount = longValue(value.get("amountMinor"));
        Instant occurredAt = instant(value.get("occurredAt"));
        ExpenseUseCase.ExpenseResult updated = expenses.update(
            tripId, expenseId, actorUserId,
            new ExpenseUseCase.ExpenseCommand(
                expenseId, text(value, "title"), text(value, "categoryCode"), amount,
                text(value, "currency"), occurredAt, null, null,
                text(value, "visibility"), text(value, "status"),
                text(value, "note"), baseVersion
            )
        );
        return result(updated);
    }

    private static TargetResult result(ExpenseUseCase.ExpenseResult expense) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", expense.title());
        fields.put("categoryCode", expense.categoryCode());
        fields.put("amountMinor", expense.amountMinor());
        fields.put("currency", expense.currency());
        fields.put("occurredAt", expense.occurredAt().toString());
        fields.put("visibility", expense.visibility());
        fields.put("status", expense.status());
        if (expense.note() != null) {
            fields.put("note", expense.note());
        }
        return new TargetResult(expense.expenseId(), Map.copyOf(fields), expense.version());
    }

    private static String text(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw EarthTripException.badRequest(
                "INVALID_RECEIPT_AMOUNT", "영수증 금액 형식이 올바르지 않습니다."
            );
        }
    }

    private static Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw EarthTripException.badRequest(
                "INVALID_RECEIPT_OCCURRED_AT", "영수증 지출 시각 형식이 올바르지 않습니다."
            );
        }
    }
}
