package com.earthtrip.expense.application.service.settlement;

import com.earthtrip.expense.application.port.in.SettlementAdjustmentUseCase;
import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.expense.application.port.out.SettlementStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SettlementAdjustmentService implements SettlementAdjustmentUseCase {

    private final TripAccess access;
    private final SettlementUseCase useCase;
    private final SettlementStorePort store;
    private final Clock clock;

    SettlementAdjustmentService(
            TripAccess access, SettlementUseCase useCase, SettlementStorePort store, Clock clock) {
        this.access = access;
        this.useCase = useCase;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferenceResult differences(UUID tripId, UUID settlementId, UUID actorUserId) {
        return compare(tripId, settlementId, actorUserId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DifferenceResult recalculate(
            UUID tripId,
            UUID settlementId,
            UUID actorUserId,
            Map<String, BigDecimal> minorUnitRates) {
        return compare(tripId, settlementId, actorUserId, minorUnitRates);
    }

    @Override
    public SupplementResult createSupplement(
            UUID tripId,
            UUID settlementId,
            UUID actorUserId,
            UUID requestId,
            long settlementBaseVersion,
            Map<String, BigDecimal> minorUnitRates) {
        access.requireEditor(tripId, actorUserId);
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        SettlementStorePort.SupplementRecord existing = store.supplement(requestId).orElse(null);
        if (existing != null) {
            if (!existing.originalSettlementId().equals(settlementId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 추가 정산에 사용된 요청 ID입니다.");
            }
            return new SupplementResult(
                    existing.id(),
                    settlementId,
                    useCase.get(tripId, existing.supplementSettlementId(), actorUserId),
                    existing.createdAt());
        }
        SettlementUseCase.SettlementResult original =
                useCase.get(tripId, settlementId, actorUserId);
        if (!original.status().equals("CLOSED")) {
            throw EarthTripException.conflict(
                    "SETTLEMENT_NOT_CLOSED", "마감된 정산에만 추가 정산을 만들 수 있습니다.");
        }
        if (original.version() != settlementBaseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 정산 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", original.version()));
        }
        DifferenceResult difference = compare(tripId, settlementId, actorUserId, minorUnitRates);
        if (!difference.hasChanges()) {
            throw EarthTripException.conflict("NO_SETTLEMENT_DIFFERENCE", "추가 정산할 변경분이 없습니다.");
        }
        SettlementUseCase.PreviewResult delta =
                new SettlementUseCase.PreviewResult(
                        original.baseCurrency(),
                        difference.netBalanceChanges(),
                        difference.transferChanges(),
                        difference.currentPreview().expenseIds(),
                        difference.currentPreview().minorUnitRates());
        Instant now = clock.instant();
        SettlementStorePort.SettlementRecord child =
                store.save(
                        new SettlementStorePort.SettlementRecord(
                                requestId,
                                tripId,
                                original.baseCurrency(),
                                "DRAFT",
                                delta,
                                0,
                                actorUserId,
                                now,
                                now,
                                null));
        SettlementStorePort.SupplementRecord relation =
                store.saveSupplement(
                        new SettlementStorePort.SupplementRecord(
                                requestId, settlementId, child.id(), actorUserId, now));
        return new SupplementResult(
                relation.id(), settlementId, result(child), relation.createdAt());
    }

    private DifferenceResult compare(
            UUID tripId,
            UUID settlementId,
            UUID actorUserId,
            Map<String, BigDecimal> requestedRates) {
        SettlementUseCase.SettlementResult original =
                useCase.get(tripId, settlementId, actorUserId);
        Map<String, BigDecimal> rates = new LinkedHashMap<>(original.snapshot().minorUnitRates());
        if (requestedRates != null) {
            rates.putAll(requestedRates);
        }
        SettlementUseCase.PreviewResult current =
                useCase.preview(tripId, actorUserId, original.baseCurrency(), rates);
        Set<UUID> oldIds = new HashSet<>(original.snapshot().expenseIds());
        Set<UUID> currentIds = new HashSet<>(current.expenseIds());
        List<UUID> added = currentIds.stream().filter(id -> !oldIds.contains(id)).sorted().toList();
        List<UUID> removed =
                oldIds.stream().filter(id -> !currentIds.contains(id)).sorted().toList();
        Map<UUID, Long> delta =
                balanceDelta(original.snapshot().netBalances(), current.netBalances());
        boolean changed =
                !added.isEmpty()
                        || !removed.isEmpty()
                        || delta.values().stream().anyMatch(value -> value != 0);
        return new DifferenceResult(
                settlementId, changed, added, removed, delta, transfers(delta), current);
    }

    private static Map<UUID, Long> balanceDelta(
            Map<UUID, Long> oldBalances, Map<UUID, Long> currentBalances) {
        Set<UUID> users = new HashSet<>(oldBalances.keySet());
        users.addAll(currentBalances.keySet());
        Map<UUID, Long> result = new LinkedHashMap<>();
        users.stream()
                .sorted()
                .forEach(
                        userId -> {
                            try {
                                result.put(
                                        userId,
                                        Math.subtractExact(
                                                currentBalances.getOrDefault(userId, 0L),
                                                oldBalances.getOrDefault(userId, 0L)));
                            } catch (ArithmeticException exception) {
                                throw EarthTripException.badRequest(
                                        "SETTLEMENT_DIFFERENCE_OVERFLOW", "정산 차액이 지원 범위를 벗어났습니다.");
                            }
                        });
        return Map.copyOf(result);
    }

    private static List<SettlementUseCase.Transfer> transfers(Map<UUID, Long> net) {
        List<Balance> creditors =
                net.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .map(entry -> new Balance(entry.getKey(), entry.getValue()))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<Balance> debtors =
                net.entrySet().stream()
                        .filter(entry -> entry.getValue() < 0)
                        .map(entry -> new Balance(entry.getKey(), -entry.getValue()))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<SettlementUseCase.Transfer> result = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Balance debtor = debtors.get(debtorIndex);
            Balance creditor = creditors.get(creditorIndex);
            long amount = Math.min(debtor.amount(), creditor.amount());
            if (amount > 0) {
                result.add(
                        new SettlementUseCase.Transfer(debtor.userId(), creditor.userId(), amount));
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

    private static SettlementUseCase.SettlementResult result(
            SettlementStorePort.SettlementRecord record) {
        return new SettlementUseCase.SettlementResult(
                record.id(),
                record.tripId(),
                record.baseCurrency(),
                record.status(),
                record.snapshot(),
                record.version(),
                record.createdBy(),
                record.createdAt(),
                record.updatedAt(),
                record.closedAt());
    }

    private record Balance(UUID userId, long amount) {}
}
