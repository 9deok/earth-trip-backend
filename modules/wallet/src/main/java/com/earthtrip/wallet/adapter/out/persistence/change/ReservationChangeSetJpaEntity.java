package com.earthtrip.wallet.adapter.out.persistence.change;

import com.earthtrip.wallet.application.port.out.ReservationChangeStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reservation_changesets")
class ReservationChangeSetJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "reservation_id", nullable = false, length = 36)
    private String reservationId;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @Column(name = "proposal_hash", nullable = false, length = 64)
    private String proposalHash;

    @Column(name = "before_snapshot", nullable = false, columnDefinition = "JSON")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", nullable = false, columnDefinition = "JSON")
    private String afterSnapshot;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected ReservationChangeSetJpaEntity() {}

    ReservationChangeSetJpaEntity(
            ReservationChangeStorePort.ChangeSetRecord record, String before, String after) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        reservationId = record.reservationId().toString();
        requestedBy = record.requestedBy().toString();
        proposalHash = record.proposalHash();
        beforeSnapshot = before;
        afterSnapshot = after;
        appliedAt = record.appliedAt();
    }

    String beforeSnapshot() {
        return beforeSnapshot;
    }

    String afterSnapshot() {
        return afterSnapshot;
    }

    ReservationChangeStorePort.ChangeSetRecord toRecord(
            Map<String, Object> before, Map<String, Object> after) {
        return new ReservationChangeStorePort.ChangeSetRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                UUID.fromString(reservationId),
                UUID.fromString(requestedBy),
                proposalHash,
                before,
                after,
                appliedAt);
    }
}
