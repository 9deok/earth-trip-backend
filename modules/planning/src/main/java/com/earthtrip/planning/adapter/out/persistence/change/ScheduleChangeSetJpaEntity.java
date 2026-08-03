package com.earthtrip.planning.adapter.out.persistence.change;

import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_changesets")
class ScheduleChangeSetJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "day_id", nullable = false, length = 36)
    private String dayId;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

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

    protected ScheduleChangeSetJpaEntity() { }

    ScheduleChangeSetJpaEntity(ScheduleChangeStorePort.ChangeSetRecord record) {
        id = record.id().toString();
        apply(record);
    }

    void apply(ScheduleChangeStorePort.ChangeSetRecord record) {
        tripId = record.tripId().toString();
        dayId = record.dayId().toString();
        requestedBy = record.requestedBy().toString();
        beforeSnapshot = record.beforeSnapshot();
        afterSnapshot = record.afterSnapshot();
        status = record.status();
        appliedAt = record.appliedAt();
        revertedAt = record.revertedAt();
    }

    ScheduleChangeStorePort.ChangeSetRecord toRecord() {
        return new ScheduleChangeStorePort.ChangeSetRecord(
            UUID.fromString(id), UUID.fromString(tripId), UUID.fromString(dayId),
            UUID.fromString(requestedBy), beforeSnapshot, afterSnapshot, status,
            appliedAt, revertedAt, version
        );
    }
}
