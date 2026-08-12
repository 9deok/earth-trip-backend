package com.earthtrip.expense.adapter.out.persistence.expense;

import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "expense_adjustments")
class ExpenseAdjustmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "expense_id", nullable = false, length = 36)
    private String expenseId;

    @Column(name = "kind", nullable = false, length = 30)
    private String kind;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "participant_id", length = 36)
    private String participantId;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExpenseAdjustmentJpaEntity() {}

    ExpenseAdjustmentJpaEntity(ExpenseStorePort.AdjustmentRecord record, String payload) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        expenseId = record.expenseId().toString();
        kind = record.kind();
        amountMinor = record.amountMinor();
        currency = record.currency();
        participantId = record.participantId() == null ? null : record.participantId().toString();
        this.payload = payload;
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
    }

    String payload() {
        return payload;
    }

    ExpenseStorePort.AdjustmentRecord toRecord(Map<String, Object> data) {
        return new ExpenseStorePort.AdjustmentRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                UUID.fromString(expenseId),
                kind,
                amountMinor,
                currency,
                participantId == null ? null : UUID.fromString(participantId),
                data,
                UUID.fromString(createdBy),
                createdAt);
    }
}
