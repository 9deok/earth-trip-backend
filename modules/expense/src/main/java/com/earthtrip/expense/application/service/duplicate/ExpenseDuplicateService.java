package com.earthtrip.expense.application.service.duplicate;

import com.earthtrip.expense.application.port.in.ExpenseDuplicateUseCase;
import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ExpenseDuplicateService implements ExpenseDuplicateUseCase {

    private final ExpenseUseCase expenses;

    ExpenseDuplicateService(ExpenseUseCase expenses) {
        this.expenses = expenses;
    }

    @Override
    public List<DuplicateResult> query(UUID tripId, UUID actorUserId, DuplicateQuery query) {
        validate(query);
        String currency = currency(query.currency());
        return expenses.list(tripId, actorUserId).stream()
                .map(expense -> score(expense, query, currency))
                .filter(result -> result.score() >= 0.5)
                .sorted(Comparator.comparingDouble(DuplicateResult::score).reversed())
                .limit(20)
                .toList();
    }

    private static DuplicateResult score(
            ExpenseUseCase.ExpenseResult expense, DuplicateQuery query, String currency) {
        double score = 0;
        List<String> reasons = new ArrayList<>();
        if (expense.amountMinor() == query.amountMinor()) {
            score += 0.5;
            reasons.add("SAME_AMOUNT");
        }
        if (expense.currency().equals(currency)) {
            score += 0.15;
            reasons.add("SAME_CURRENCY");
        }
        long minutes =
                Math.abs(Duration.between(expense.occurredAt(), query.occurredAt()).toMinutes());
        if (minutes <= 5) {
            score += 0.2;
            reasons.add("NEAR_SAME_TIME");
        } else if (minutes <= 24 * 60) {
            score += 0.1;
            reasons.add("SAME_DAY_WINDOW");
        }
        String left = normalize(expense.title());
        String right = normalize(query.title());
        if (left.equals(right)) {
            score += 0.15;
            reasons.add("SAME_TITLE");
        } else if (left.contains(right) || right.contains(left)) {
            score += 0.08;
            reasons.add("SIMILAR_TITLE");
        }
        return new DuplicateResult(
                expense.expenseId(), Math.min(1, score), List.copyOf(reasons), expense);
    }

    private static void validate(DuplicateQuery query) {
        if (query == null
                || query.title() == null
                || query.title().isBlank()
                || query.title().strip().length() > 200
                || query.amountMinor() <= 0
                || query.occurredAt() == null) {
            throw EarthTripException.badRequest(
                    "INVALID_EXPENSE_DUPLICATE_QUERY", "중복을 찾을 지출 정보를 확인해 주세요.");
        }
    }

    private static String currency(String value) {
        try {
            return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (RuntimeException exception) {
            throw EarthTripException.badRequest("INVALID_CURRENCY", "유효한 ISO 4217 통화 코드가 아닙니다.");
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
