package com.earthtrip.expense.application.service.settlement;

import com.earthtrip.expense.application.port.in.SettlementUseCase.PreviewResult;
import com.earthtrip.expense.application.port.in.SettlementUseCase.Transfer;
import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.domain.Expense;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

final class SettlementPreviewCalculator {
    private final ExpenseStorePort expenses;

    SettlementPreviewCalculator(ExpenseStorePort expenses) {
        this.expenses = expenses;
    }

    PreviewResult calculate(
            UUID tripId, String baseCurrency, Map<String, BigDecimal> exchangeRates) {
        String base = currency(baseCurrency);
        Map<String, BigDecimal> normalized = rates(exchangeRates, base);
        Map<UUID, Long> net = new LinkedHashMap<>();
        List<UUID> expenseIds = new ArrayList<>();
        for (Expense expense : expenses.findAll(tripId)) {
            if (!expense.status().equals("RECORDED")) {
                continue;
            }
            BigDecimal rate = normalized.get(expense.currency());
            if (rate == null || rate.signum() <= 0) {
                throw EarthTripException.badRequest(
                        "EXCHANGE_RATE_REQUIRED", "통화 " + expense.currency() + "의 최소단위 환율이 필요합니다.");
            }
            expenseIds.add(expense.id());
            AdjustedAmounts adjusted = adjustedAmounts(expense);
            adjusted.payers()
                    .forEach(
                            (userId, amount) ->
                                    net.merge(userId, convert(amount, rate), Math::addExact));
            adjusted.shares()
                    .forEach(
                            (userId, amount) ->
                                    net.merge(userId, -convert(amount, rate), Math::addExact));
        }
        correctRoundingDrift(net);
        return new PreviewResult(
                base,
                Map.copyOf(net),
                transfers(net),
                List.copyOf(expenseIds),
                Map.copyOf(normalized));
    }

    private AdjustedAmounts adjustedAmounts(Expense expense) {
        Map<UUID, Long> payers = new LinkedHashMap<>(expense.payers());
        Map<UUID, Long> shares = new LinkedHashMap<>(expense.shares());
        long refunded = 0;
        for (ExpenseStorePort.AdjustmentRecord adjustment :
                expenses.findAdjustments(expense.id())) {
            if (!adjustment.kind().equals("REFUND")) {
                continue;
            }
            if (!adjustment.currency().equals(expense.currency())) {
                throw new IllegalStateException("환불 통화와 원본 지출 통화가 다릅니다.");
            }
            refunded = Math.addExact(refunded, adjustment.amountMinor());
            if (refunded > expense.amountMinor()) {
                throw new IllegalStateException("누적 환불 금액이 원본 지출을 초과합니다.");
            }
            reduce(payers, adjustment.amountMinor(), payerId(adjustment, payers));
            reduce(shares, adjustment.amountMinor(), adjustment.participantId());
        }
        return new AdjustedAmounts(Map.copyOf(payers), Map.copyOf(shares));
    }

    private static UUID payerId(
            ExpenseStorePort.AdjustmentRecord adjustment, Map<UUID, Long> payers) {
        Object value = adjustment.payload().get("payerUserId");
        if (value == null) {
            return null;
        }
        try {
            UUID payerId = UUID.fromString(String.valueOf(value));
            return payers.containsKey(payerId) ? payerId : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void reduce(Map<UUID, Long> amounts, long reduction, UUID preferred) {
        long remaining = reduction;
        if (preferred != null && amounts.containsKey(preferred)) {
            remaining = reduceOne(amounts, preferred, remaining);
        }
        for (UUID userId : amounts.keySet().stream().sorted().toList()) {
            if (remaining == 0) {
                break;
            }
            if (!userId.equals(preferred)) {
                remaining = reduceOne(amounts, userId, remaining);
            }
        }
        if (remaining != 0) {
            throw new IllegalStateException("환불 금액을 분담 정보에 반영할 수 없습니다.");
        }
    }

    private static long reduceOne(Map<UUID, Long> amounts, UUID userId, long remaining) {
        long current = amounts.get(userId);
        long applied = Math.min(current, remaining);
        amounts.put(userId, current - applied);
        return remaining - applied;
    }

    private static Map<String, BigDecimal> rates(
            Map<String, BigDecimal> values, String baseCurrency) {
        Map<String, BigDecimal> normalized = new TreeMap<>();
        if (values != null) {
            values.forEach(
                    (code, rate) -> {
                        if (rate == null || rate.signum() <= 0) {
                            throw EarthTripException.badRequest(
                                    "INVALID_EXCHANGE_RATE", "환율은 0보다 커야 합니다.");
                        }
                        normalized.put(currency(code), rate);
                    });
        }
        normalized.put(baseCurrency, BigDecimal.ONE);
        return normalized;
    }

    private static void correctRoundingDrift(Map<UUID, Long> net) {
        long drift = net.values().stream().reduce(0L, Math::addExact);
        if (drift != 0 && !net.isEmpty()) {
            UUID userId = net.keySet().stream().sorted().findFirst().orElseThrow();
            net.merge(userId, -drift, Math::addExact);
        }
    }

    private static List<Transfer> transfers(Map<UUID, Long> net) {
        List<Balance> creditors = balances(net, true);
        List<Balance> debtors = balances(net, false);
        List<Transfer> result = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Balance debtor = debtors.get(debtorIndex);
            Balance creditor = creditors.get(creditorIndex);
            long amount = Math.min(debtor.amount(), creditor.amount());
            if (amount > 0) {
                result.add(new Transfer(debtor.userId(), creditor.userId(), amount));
            }
            long debtorLeft = debtor.amount() - amount;
            long creditorLeft = creditor.amount() - amount;
            debtors.set(debtorIndex, new Balance(debtor.userId(), debtorLeft));
            creditors.set(creditorIndex, new Balance(creditor.userId(), creditorLeft));
            if (debtorLeft == 0) {
                debtorIndex++;
            }
            if (creditorLeft == 0) {
                creditorIndex++;
            }
        }
        return List.copyOf(result);
    }

    private static List<Balance> balances(Map<UUID, Long> net, boolean positive) {
        return net.entrySet().stream()
                .filter(entry -> positive ? entry.getValue() > 0 : entry.getValue() < 0)
                .sorted(Map.Entry.comparingByKey())
                .map(
                        entry ->
                                new Balance(
                                        entry.getKey(),
                                        positive ? entry.getValue() : -entry.getValue()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static long convert(long amount, BigDecimal rate) {
        try {
            return BigDecimal.valueOf(amount)
                    .multiply(rate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw EarthTripException.badRequest(
                    "SETTLEMENT_AMOUNT_OVERFLOW", "환산된 정산 금액이 지원 범위를 벗어났습니다.");
        }
    }

    private static String currency(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw EarthTripException.badRequest("INVALID_CURRENCY", "통화 코드는 영문 3자리여야 합니다.");
        }
        return normalized;
    }

    private record AdjustedAmounts(Map<UUID, Long> payers, Map<UUID, Long> shares) {}

    private record Balance(UUID userId, long amount) {}
}
