package com.earthtrip.expense.adapter.out.persistence.settlement;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.expense.application.port.out.SettlementStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SettlementPersistenceAdapter implements SettlementStorePort {

    private final SettlementJpaRepository settlements;
    private final SettlementPaymentJpaRepository payments;
    private final SettlementSupplementJpaRepository supplements;
    private final ObjectMapper json;

    SettlementPersistenceAdapter(
            SettlementJpaRepository settlements,
            SettlementPaymentJpaRepository payments,
            SettlementSupplementJpaRepository supplements,
            ObjectMapper json) {
        this.settlements = settlements;
        this.payments = payments;
        this.supplements = supplements;
        this.json = json;
    }

    @Override
    public List<SettlementRecord> findAll(UUID tripId) {
        return settlements.findAllByTripIdOrderByCreatedAtDesc(tripId.toString()).stream()
                .map(this::record)
                .toList();
    }

    @Override
    public Optional<SettlementRecord> findById(UUID settlementId) {
        return settlements.findById(settlementId.toString()).map(this::record);
    }

    @Override
    public SettlementRecord save(SettlementRecord record) {
        String snapshot = write(record.snapshot());
        SettlementJpaEntity entity =
                settlements
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, snapshot);
                                    return existing;
                                })
                        .orElseGet(() -> new SettlementJpaEntity(record, snapshot));
        return record(settlements.saveAndFlush(entity));
    }

    @Override
    public List<PaymentRecord> payments(UUID settlementId) {
        return payments.findAllBySettlementIdOrderByCreatedAtAsc(settlementId.toString()).stream()
                .map(SettlementPaymentJpaEntity::record)
                .toList();
    }

    @Override
    public Optional<PaymentRecord> payment(UUID paymentId) {
        return payments.findById(paymentId.toString()).map(SettlementPaymentJpaEntity::record);
    }

    @Override
    public PaymentRecord savePayment(PaymentRecord record) {
        SettlementPaymentJpaEntity entity =
                payments.findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record);
                                    return existing;
                                })
                        .orElseGet(() -> new SettlementPaymentJpaEntity(record));
        return payments.saveAndFlush(entity).record();
    }

    @Override
    public void deletePayment(UUID paymentId) {
        payments.deleteById(paymentId.toString());
    }

    @Override
    public Optional<SupplementRecord> supplement(UUID supplementId) {
        return supplements
                .findById(supplementId.toString())
                .map(SettlementSupplementJpaEntity::toRecord);
    }

    @Override
    public SupplementRecord saveSupplement(SupplementRecord record) {
        return supplements.save(new SettlementSupplementJpaEntity(record)).toRecord();
    }

    private SettlementRecord record(SettlementJpaEntity entity) {
        try {
            return entity.record(
                    json.readValue(entity.snapshot(), SettlementUseCase.PreviewResult.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("정산 스냅샷을 읽을 수 없습니다.", exception);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("정산 스냅샷을 저장할 수 없습니다.", exception);
        }
    }
}
