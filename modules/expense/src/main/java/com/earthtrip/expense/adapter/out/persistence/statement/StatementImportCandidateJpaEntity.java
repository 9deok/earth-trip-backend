package com.earthtrip.expense.adapter.out.persistence.statement;

import com.earthtrip.expense.application.port.out.StatementImportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_import_candidates")
class StatementImportCandidateJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "import_id", nullable = false, length = 36)
    private String importId;
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "payer_user_id", nullable = false, length = 36)
    private String payerUserId;
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "expense_id", length = 36)
    private String expenseId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected StatementImportCandidateJpaEntity() { }

    StatementImportCandidateJpaEntity(
        StatementImportStorePort.CandidateRecord record,
        String payload
    ) {
        id = record.id().toString();
        importId = record.importId().toString();
        tripId = record.tripId().toString();
        createdAt = record.createdAt();
        apply(record, payload);
    }

    void apply(StatementImportStorePort.CandidateRecord record, String payload) {
        title = record.title();
        amountMinor = record.amountMinor();
        currency = record.currency();
        occurredAt = record.occurredAt();
        payerUserId = record.payerUserId().toString();
        this.payload = payload;
        status = record.status();
        expenseId = record.expenseId() == null ? null : record.expenseId().toString();
        updatedAt = record.updatedAt();
    }

    String payload() { return payload; }

    StatementImportStorePort.CandidateRecord toRecord(java.util.Map<String, Object> data) {
        return new StatementImportStorePort.CandidateRecord(
            UUID.fromString(id), UUID.fromString(importId), UUID.fromString(tripId), title,
            amountMinor, currency, occurredAt, UUID.fromString(payerUserId), data, status,
            expenseId == null ? null : UUID.fromString(expenseId), createdAt, updatedAt, version
        );
    }
}
