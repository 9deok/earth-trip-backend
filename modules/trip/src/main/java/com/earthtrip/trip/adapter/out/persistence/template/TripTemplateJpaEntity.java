package com.earthtrip.trip.adapter.out.persistence.template;

import com.earthtrip.trip.application.port.out.TripTemplateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "trip_templates")
class TripTemplateJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "source_trip_id", nullable = false, length = 36)
    private String sourceTripId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "include_scopes", nullable = false, columnDefinition = "JSON")
    private String includeScopes;

    @Column(name = "snapshot", nullable = false, columnDefinition = "JSON")
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TripTemplateJpaEntity() {}

    TripTemplateJpaEntity(
            TripTemplateStorePort.TemplateRecord record, String scopes, String snapshot) {
        id = record.id().toString();
        ownerUserId = record.ownerUserId().toString();
        sourceTripId = record.sourceTripId().toString();
        createdAt = record.createdAt();
        apply(record, scopes, snapshot);
    }

    void apply(TripTemplateStorePort.TemplateRecord record, String scopes, String snapshot) {
        name = record.name();
        description = record.description();
        includeScopes = scopes;
        this.snapshot = snapshot;
        updatedAt = record.updatedAt();
        deletedAt = record.deletedAt();
    }

    String includeScopes() {
        return includeScopes;
    }

    String snapshot() {
        return snapshot;
    }

    TripTemplateStorePort.TemplateRecord toRecord(
            Set<String> scopes, Map<String, Object> snapshotData) {
        return new TripTemplateStorePort.TemplateRecord(
                UUID.fromString(id),
                UUID.fromString(ownerUserId),
                UUID.fromString(sourceTripId),
                name,
                description,
                scopes,
                snapshotData,
                createdAt,
                updatedAt,
                deletedAt,
                version);
    }
}
