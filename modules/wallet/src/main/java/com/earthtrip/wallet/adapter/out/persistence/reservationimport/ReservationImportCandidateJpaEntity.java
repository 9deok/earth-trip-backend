package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reservation_import_candidates")
class ReservationImportCandidateJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "candidate_type", nullable = false, length = 40)
    private String candidateType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "reservation_id", length = 36)
    private String reservationId;

    @Column(name = "dismissal_reason", length = 500)
    private String dismissalReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReservationImportCandidateJpaEntity() {}

    ReservationImportCandidateJpaEntity(
            ReservationImportStorePort.CandidateRecord record, String payload) {
        id = record.id().toString();
        jobId = record.jobId().toString();
        tripId = record.tripId().toString();
        createdAt = record.createdAt();
        apply(record, payload);
    }

    void apply(ReservationImportStorePort.CandidateRecord record, String payload) {
        title = record.title();
        candidateType = record.candidateType();
        this.payload = payload;
        confidence = record.confidence();
        status = record.status();
        reservationId = record.reservationId() == null ? null : record.reservationId().toString();
        dismissalReason = record.dismissalReason();
        updatedAt = record.updatedAt();
    }

    String payload() {
        return payload;
    }

    ReservationImportStorePort.CandidateRecord toRecord(Map<String, Object> data) {
        return new ReservationImportStorePort.CandidateRecord(
                UUID.fromString(id),
                UUID.fromString(jobId),
                UUID.fromString(tripId),
                title,
                candidateType,
                data,
                confidence,
                status,
                reservationId == null ? null : UUID.fromString(reservationId),
                dismissalReason,
                createdAt,
                updatedAt,
                version);
    }
}
