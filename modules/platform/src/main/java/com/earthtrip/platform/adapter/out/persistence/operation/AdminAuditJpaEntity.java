package com.earthtrip.platform.adapter.out.persistence.operation;

import com.earthtrip.platform.application.port.out.OperationalStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_events")
class AdminAuditJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id", nullable = false)
    private Long sequenceId;
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;
    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;
    @Column(name = "actor_id", length = 160)
    private String actorId;
    @Column(name = "action", nullable = false, length = 80)
    private String action;
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;
    @Column(name = "target_id", length = 160)
    private String targetId;
    @Column(name = "outcome", nullable = false, length = 30)
    private String outcome;
    @Column(name = "metadata", nullable = false, columnDefinition = "JSON")
    private String metadata;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AdminAuditJpaEntity() { }

    AdminAuditJpaEntity(OperationalStorePort.AuditRecord record, String metadata) {
        eventId = record.eventId().toString();
        actorType = record.actorType();
        actorId = record.actorId();
        action = record.action();
        targetType = record.targetType();
        targetId = record.targetId();
        outcome = record.outcome();
        this.metadata = metadata;
        occurredAt = record.occurredAt();
    }

    String metadata() {
        return metadata;
    }

    OperationalStorePort.AuditRecord record(Map<String, Object> metadataValue) {
        return new OperationalStorePort.AuditRecord(
            sequenceId,
            UUID.fromString(eventId),
            actorType,
            actorId,
            action,
            targetType,
            targetId,
            outcome,
            metadataValue,
            occurredAt
        );
    }
}
