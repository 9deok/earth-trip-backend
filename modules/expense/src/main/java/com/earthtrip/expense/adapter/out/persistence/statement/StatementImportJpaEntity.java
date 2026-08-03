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
@Table(name = "statement_imports")
class StatementImportJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;
    @Column(name = "source", nullable = false, length = 80)
    private String source;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected StatementImportJpaEntity() { }

    StatementImportJpaEntity(StatementImportStorePort.ImportRecord record) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        source = record.source();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
        apply(record);
    }

    void apply(StatementImportStorePort.ImportRecord record) {
        status = record.status();
        updatedAt = record.updatedAt();
    }

    StatementImportStorePort.ImportRecord toRecord() {
        return new StatementImportStorePort.ImportRecord(
            UUID.fromString(id), UUID.fromString(tripId), source, status,
            UUID.fromString(createdBy), createdAt, updatedAt, version
        );
    }
}
