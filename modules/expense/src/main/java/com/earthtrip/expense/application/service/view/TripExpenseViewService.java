package com.earthtrip.expense.application.service.view;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripExpenseViewService implements TripExpenseView {

    private final ExpenseUseCase expenses;

    TripExpenseViewService(ExpenseUseCase expenses) {
        this.expenses = expenses;
    }

    @Override
    public ExpenseSummary summary(UUID tripId, UUID actorUserId) {
        List<ExpenseUseCase.ExpenseResult> visible = expenses.list(tripId, actorUserId);
        Map<String, Long> totals =
                visible.stream()
                        .filter(expense -> !expense.status().equals("DELETED"))
                        .collect(
                                Collectors.groupingBy(
                                        ExpenseUseCase.ExpenseResult::currency,
                                        TreeMap::new,
                                        Collectors.summingLong(
                                                ExpenseUseCase.ExpenseResult::amountMinor)));
        int provisional =
                (int)
                        visible.stream()
                                .filter(
                                        expense ->
                                                Set.of("DRAFT", "PROVISIONAL")
                                                        .contains(expense.status()))
                                .count();
        return new ExpenseSummary(
                visible.size(),
                provisional,
                totals.entrySet().stream()
                        .map(entry -> new Total(entry.getKey(), entry.getValue()))
                        .toList());
    }

    @Override
    public List<Entry> searchEntries(UUID tripId, UUID actorUserId) {
        return expenses.list(tripId, actorUserId).stream()
                .map(
                        expense ->
                                new Entry(
                                        expense.expenseId(),
                                        expense.title(),
                                        expense.categoryCode(),
                                        expense.amountMinor(),
                                        expense.currency(),
                                        expense.occurredAt(),
                                        expense.note(),
                                        expense.status()))
                .toList();
    }
}
