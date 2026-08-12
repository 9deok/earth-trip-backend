package com.earthtrip.expense.application.port.out;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import java.time.Instant;
import java.util.*;

public interface SettlementStorePort {
    List<SettlementRecord> findAll(UUID trip);

    Optional<SettlementRecord> findById(UUID id);

    SettlementRecord save(SettlementRecord r);

    List<PaymentRecord> payments(UUID settlement);

    Optional<PaymentRecord> payment(UUID id);

    PaymentRecord savePayment(PaymentRecord r);

    void deletePayment(UUID id);

    Optional<SupplementRecord> supplement(UUID id);

    SupplementRecord saveSupplement(SupplementRecord r);

    record SettlementRecord(
            UUID id,
            UUID tripId,
            String baseCurrency,
            String status,
            SettlementUseCase.PreviewResult snapshot,
            long version,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt) {}

    record PaymentRecord(
            UUID id,
            UUID settlementId,
            UUID fromUserId,
            UUID toUserId,
            long amountMinor,
            String currency,
            Instant paidAt,
            String note,
            long version,
            Instant createdAt,
            Instant updatedAt) {}

    record SupplementRecord(
            UUID id,
            UUID originalSettlementId,
            UUID supplementSettlementId,
            UUID createdBy,
            Instant createdAt) {}
}
