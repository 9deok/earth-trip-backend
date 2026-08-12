package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.TripShareStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_share_links")
class TripShareLinkJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "scopes_json", nullable = false, columnDefinition = "JSON")
    private String scopes;

    @Column(name = "password_hash", length = 500)
    private String passwordHash;

    @Column(name = "projection_user_id", nullable = false, length = 36)
    private String projectionUserId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TripShareLinkJpaEntity() {}

    TripShareLinkJpaEntity(TripShareStorePort.ShareRecord record, String scopes) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        tokenHash = record.tokenHash();
        projectionUserId = record.projectionUserId().toString();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
        apply(record, scopes);
    }

    void apply(TripShareStorePort.ShareRecord record, String scopes) {
        name = record.name();
        this.scopes = scopes;
        passwordHash = record.passwordHash();
        expiresAt = record.expiresAt();
        status = record.status();
        updatedAt = record.updatedAt();
        revokedAt = record.revokedAt();
    }

    String scopes() {
        return scopes;
    }

    TripShareStorePort.ShareRecord toRecord(java.util.List<String> scopeList) {
        return new TripShareStorePort.ShareRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                tokenHash,
                name,
                scopeList,
                passwordHash,
                UUID.fromString(projectionUserId),
                expiresAt,
                status,
                UUID.fromString(createdBy),
                createdAt,
                updatedAt,
                revokedAt,
                version);
    }
}
