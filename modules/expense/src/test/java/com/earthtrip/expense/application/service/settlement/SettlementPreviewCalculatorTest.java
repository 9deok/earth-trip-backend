package com.earthtrip.expense.application.service.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.domain.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettlementPreviewCalculatorTest {

    @Test
    void 환불을_반영한_순액과_송금안을_계산한다() {
        UUID tripId = UUID.randomUUID();
        UUID payer = UUID.randomUUID();
        UUID participant = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-06T00:00:00Z");
        Expense expense =
                Expense.create(
                        expenseId,
                        tripId,
                        "숙소",
                        "ACCOMMODATION",
                        1_000,
                        "KRW",
                        now,
                        Map.of(payer, 1_000L),
                        Map.of(participant, 1_000L),
                        "TRIP",
                        null,
                        payer,
                        now);
        ExpenseStorePort store = mock(ExpenseStorePort.class);
        when(store.findAll(tripId)).thenReturn(List.of(expense));
        when(store.findAdjustments(expenseId))
                .thenReturn(
                        List.of(
                                new ExpenseStorePort.AdjustmentRecord(
                                        UUID.randomUUID(),
                                        tripId,
                                        expenseId,
                                        "REFUND",
                                        200,
                                        "KRW",
                                        participant,
                                        Map.of("payerUserId", payer.toString()),
                                        payer,
                                        now)));

        var preview =
                new SettlementPreviewCalculator(store)
                        .calculate(tripId, "KRW", Map.of("KRW", BigDecimal.ONE));

        assertThat(preview.netBalances())
                .containsEntry(payer, 800L)
                .containsEntry(participant, -800L);
        assertThat(preview.transfers())
                .singleElement()
                .satisfies(
                        transfer -> {
                            assertThat(transfer.fromUserId()).isEqualTo(participant);
                            assertThat(transfer.toUserId()).isEqualTo(payer);
                            assertThat(transfer.amountMinor()).isEqualTo(800L);
                        });
    }
}
