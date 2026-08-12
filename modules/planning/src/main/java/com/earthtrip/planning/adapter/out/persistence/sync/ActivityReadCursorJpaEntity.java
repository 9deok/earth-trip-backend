package com.earthtrip.planning.adapter.out.persistence.sync;

import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_read_cursors")
@IdClass(ActivityReadCursorId.class)
class ActivityReadCursorJpaEntity {

    @Id
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "sequence_id", nullable = false)
    private long sequenceId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActivityReadCursorJpaEntity() {}

    ActivityReadCursorJpaEntity(SyncStateStorePort.ReadCursorRecord record) {
        tripId = record.tripId().toString();
        userId = record.userId().toString();
        apply(record);
    }

    void apply(SyncStateStorePort.ReadCursorRecord record) {
        sequenceId = record.sequenceId();
        updatedAt = record.updatedAt();
    }

    SyncStateStorePort.ReadCursorRecord toRecord() {
        return new SyncStateStorePort.ReadCursorRecord(
                UUID.fromString(tripId), UUID.fromString(userId), sequenceId, updatedAt);
    }
}
