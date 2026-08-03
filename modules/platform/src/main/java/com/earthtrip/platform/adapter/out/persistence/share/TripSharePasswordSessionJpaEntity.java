package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.TripShareStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_share_password_sessions")
class TripSharePasswordSessionJpaEntity {
    @Id @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "share_id", nullable = false, length = 36) private String shareId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected TripSharePasswordSessionJpaEntity() { }
    TripSharePasswordSessionJpaEntity(TripShareStorePort.PasswordSessionRecord record) {
        tokenHash = record.tokenHash(); shareId = record.shareId().toString();
        expiresAt = record.expiresAt(); createdAt = record.createdAt();
    }
    TripShareStorePort.PasswordSessionRecord toRecord() {
        return new TripShareStorePort.PasswordSessionRecord(
            tokenHash, UUID.fromString(shareId), expiresAt, createdAt
        );
    }
}
