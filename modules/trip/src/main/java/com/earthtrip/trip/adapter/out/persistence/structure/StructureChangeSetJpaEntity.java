package com.earthtrip.trip.adapter.out.persistence.structure;

import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_structure_changesets")
class StructureChangeSetJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @Column(name = "proposal_hash", nullable = false, length = 64)
    private String proposalHash;

    @Column(name = "before_snapshot", nullable = false, columnDefinition = "JSON")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", nullable = false, columnDefinition = "JSON")
    private String afterSnapshot;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected StructureChangeSetJpaEntity() {}

    StructureChangeSetJpaEntity(TripStructureStorePort.ChangeSetRecord record) {
        id = record.id().toString();
        apply(record);
    }

    void apply(TripStructureStorePort.ChangeSetRecord record) {
        tripId = record.tripId().toString();
        requestedBy = record.requestedBy().toString();
        proposalHash = record.proposalHash();
        beforeSnapshot = record.beforeSnapshot();
        afterSnapshot = record.afterSnapshot();
        status = record.status();
        appliedAt = record.appliedAt();
        revertedAt = record.revertedAt();
    }

    TripStructureStorePort.ChangeSetRecord toRecord() {
        return new TripStructureStorePort.ChangeSetRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                UUID.fromString(requestedBy),
                proposalHash,
                beforeSnapshot,
                afterSnapshot,
                status,
                appliedAt,
                revertedAt,
                version);
    }
}
