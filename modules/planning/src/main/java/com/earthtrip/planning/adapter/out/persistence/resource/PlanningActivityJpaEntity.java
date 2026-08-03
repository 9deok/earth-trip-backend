package com.earthtrip.planning.adapter.out.persistence.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planning_activity_events")
class PlanningActivityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Long sequenceId;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PlanningActivityJpaEntity() { }

    PlanningActivityJpaEntity(
        UUID tripId,
        UUID actorId,
        String action,
        String resourceType,
        UUID resourceId,
        String payload,
        Instant occurredAt
    ) {
        eventId = UUID.randomUUID().toString();
        this.tripId = tripId.toString();
        this.actorId = actorId.toString();
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId.toString();
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    long sequenceId() {
        return sequenceId;
    }

    UUID eventId() {
        return UUID.fromString(eventId);
    }

    UUID tripId() {
        return UUID.fromString(tripId);
    }

    UUID actorId() {
        return UUID.fromString(actorId);
    }

    String action() {
        return action;
    }

    String resourceType() {
        return resourceType;
    }

    UUID resourceId() {
        return UUID.fromString(resourceId);
    }

    String payload() {
        return payload;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
