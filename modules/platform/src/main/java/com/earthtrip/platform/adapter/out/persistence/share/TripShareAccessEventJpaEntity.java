package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.TripShareStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_share_access_events")
class TripShareAccessEventJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id") private Long sequenceId;
    @Column(name = "event_id", nullable = false, length = 36) private String eventId;
    @Column(name = "share_id", nullable = false, length = 36) private String shareId;
    @Column(name = "success", nullable = false) private boolean success;
    @Column(name = "reason", nullable = false, length = 50) private String reason;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    protected TripShareAccessEventJpaEntity() { }
    TripShareAccessEventJpaEntity(TripShareStorePort.AccessRecord record) {
        eventId = record.eventId().toString(); shareId = record.shareId().toString();
        success = record.success(); reason = record.reason(); occurredAt = record.occurredAt();
    }
    TripShareStorePort.AccessRecord toRecord() {
        return new TripShareStorePort.AccessRecord(
            UUID.fromString(eventId), UUID.fromString(shareId), success, reason, occurredAt
        );
    }
}
