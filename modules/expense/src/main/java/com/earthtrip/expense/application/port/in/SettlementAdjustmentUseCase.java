package com.earthtrip.expense.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SettlementAdjustmentUseCase {

    DifferenceResult differences(UUID tripId, UUID settlementId, UUID actorUserId);

    DifferenceResult recalculate(
        UUID tripId,
        UUID settlementId,
        UUID actorUserId,
        Map<String, BigDecimal> minorUnitRates
    );

    SupplementResult createSupplement(
        UUID tripId,
        UUID settlementId,
        UUID actorUserId,
        UUID requestId,
        long settlementBaseVersion,
        Map<String, BigDecimal> minorUnitRates
    );

    record DifferenceResult(
        UUID settlementId,
        boolean hasChanges,
        List<UUID> addedExpenseIds,
        List<UUID> removedExpenseIds,
        Map<UUID, Long> netBalanceChanges,
        List<SettlementUseCase.Transfer> transferChanges,
        SettlementUseCase.PreviewResult currentPreview
    ) { }

    record SupplementResult(
        UUID supplementId,
        UUID originalSettlementId,
        SettlementUseCase.SettlementResult settlement,
        Instant createdAt
    ) { }
}
