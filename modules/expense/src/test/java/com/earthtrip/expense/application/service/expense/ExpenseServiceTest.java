package com.earthtrip.expense.application.service.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.expense.application.port.in.ExpenseCategoryUseCase;
import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.domain.Expense;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.spi.TripChangePublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpenseServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 다른_사용자의_비공개_지출은_ID를_알아도_삭제할_수_없다() {
        UUID tripId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        Expense expense =
                Expense.create(
                        expenseId,
                        tripId,
                        "개인 결제",
                        "OTHER",
                        10_000,
                        "KRW",
                        NOW,
                        Map.of(owner, 10_000L),
                        Map.of(owner, 10_000L),
                        "PRIVATE",
                        null,
                        owner,
                        NOW);
        ExpenseStorePort store = mock(ExpenseStorePort.class);
        when(store.findById(expenseId)).thenReturn(Optional.of(expense));
        ExpenseService service =
                new ExpenseService(
                        mock(TripAccess.class),
                        store,
                        mock(ExpenseCategoryUseCase.class),
                        mock(TripChangePublisher.class),
                        Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.delete(tripId, expenseId, attacker, 0))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("EXPENSE_NOT_FOUND"));

        verify(store, never()).save(expense);
    }

    @Test
    void 다른_사용자의_지출_ID는_멱등키로_재사용해_내용을_조회할_수_없다() {
        UUID tripId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        Expense expense =
                Expense.create(
                        expenseId,
                        tripId,
                        "개인 결제",
                        "OTHER",
                        10_000,
                        "KRW",
                        NOW,
                        Map.of(owner, 10_000L),
                        Map.of(owner, 10_000L),
                        "PRIVATE",
                        "비공개 메모",
                        owner,
                        NOW);
        ExpenseStorePort store = mock(ExpenseStorePort.class);
        when(store.findById(expenseId)).thenReturn(Optional.of(expense));
        ExpenseService service =
                new ExpenseService(
                        mock(TripAccess.class),
                        store,
                        mock(ExpenseCategoryUseCase.class),
                        mock(TripChangePublisher.class),
                        Clock.fixed(NOW, ZoneOffset.UTC));
        ExpenseUseCase.ExpenseCommand command =
                new ExpenseUseCase.ExpenseCommand(
                        expenseId,
                        "공격자 요청",
                        "OTHER",
                        10_000L,
                        "KRW",
                        NOW,
                        Map.of(attacker, 10_000L),
                        Map.of(attacker, 10_000L),
                        "TRIP",
                        null,
                        null,
                        0);

        assertThatThrownBy(() -> service.create(tripId, attacker, command))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));

        verify(store, never()).save(expense);
    }
}
