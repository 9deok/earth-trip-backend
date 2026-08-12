package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reservation_import_jobs")
class ReservationImportJobJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_payload", nullable = false, columnDefinition = "JSON")
    private String sourcePayload;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReservationImportJobJpaEntity() {}

    ReservationImportJobJpaEntity(
            ReservationImportStorePort.JobRecord record, String sourcePayload) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        sourceType = record.sourceType();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
        apply(record, sourcePayload);
    }

    void apply(ReservationImportStorePort.JobRecord record, String sourcePayload) {
        this.sourcePayload = sourcePayload;
        status = record.status();
        failureCode = record.failureCode();
        failureMessage = record.failureMessage();
        attemptCount = record.attemptCount();
        updatedAt = record.updatedAt();
    }

    String sourcePayload() {
        return sourcePayload;
    }

    ReservationImportStorePort.JobRecord toRecord(Map<String, Object> payload) {
        return new ReservationImportStorePort.JobRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                sourceType,
                payload,
                status,
                failureCode,
                failureMessage,
                attemptCount,
                UUID.fromString(createdBy),
                createdAt,
                updatedAt,
                version);
    }
}
