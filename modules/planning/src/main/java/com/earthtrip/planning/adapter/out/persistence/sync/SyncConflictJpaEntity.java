package com.earthtrip.planning.adapter.out.persistence.sync;

import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_conflicts")
class SyncConflictJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Column(name = "device_command", nullable = false, columnDefinition = "JSON")
    private String deviceCommand;

    @Column(name = "server_snapshot", columnDefinition = "JSON")
    private String serverSnapshot;

    @Column(name = "mergeable_fields", nullable = false, columnDefinition = "JSON")
    private String mergeableFields;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "resolution", length = 30)
    private String resolution;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SyncConflictJpaEntity() {}

    SyncConflictJpaEntity(
            SyncStateStorePort.ConflictRecord record,
            String deviceCommand,
            String serverSnapshot,
            String mergeableFields) {
        id = record.conflictId().toString();
        operationId = record.operationId().toString();
        apply(record, deviceCommand, serverSnapshot, mergeableFields);
    }

    void apply(
            SyncStateStorePort.ConflictRecord record,
            String deviceCommand,
            String serverSnapshot,
            String mergeableFields) {
        tripId = record.tripId().toString();
        actorId = record.actorId().toString();
        action = record.action();
        resourceType = record.resourceType();
        resourceId = record.resourceId().toString();
        this.deviceCommand = deviceCommand;
        this.serverSnapshot = serverSnapshot;
        this.mergeableFields = mergeableFields;
        status = record.status();
        resolution = record.resolution();
        createdAt = record.createdAt();
        resolvedAt = record.resolvedAt();
    }

    UUID id() {
        return UUID.fromString(id);
    }

    UUID operationId() {
        return UUID.fromString(operationId);
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

    String deviceCommand() {
        return deviceCommand;
    }

    String serverSnapshot() {
        return serverSnapshot;
    }

    String mergeableFields() {
        return mergeableFields;
    }

    String status() {
        return status;
    }

    String resolution() {
        return resolution;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant resolvedAt() {
        return resolvedAt;
    }

    long version() {
        return version;
    }
}
