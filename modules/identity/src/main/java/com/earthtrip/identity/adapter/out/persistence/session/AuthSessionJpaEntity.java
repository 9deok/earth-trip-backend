package com.earthtrip.identity.adapter.out.persistence.session;

import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "access_token_hash", nullable = false, length = 64, unique = true)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false, length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "device_name", nullable = false, length = 120)
    private String deviceName;

    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuthSessionJpaEntity() {}

    private AuthSessionJpaEntity(AuthSession session) {
        id = session.id().toString();
        apply(session);
    }

    static AuthSessionJpaEntity from(AuthSession session) {
        return new AuthSessionJpaEntity(session);
    }

    void apply(AuthSession session) {
        userId = session.userId().toString();
        accessTokenHash = session.accessTokenHash();
        refreshTokenHash = session.refreshTokenHash();
        deviceName = session.deviceName();
        accessExpiresAt = session.accessExpiresAt();
        refreshExpiresAt = session.refreshExpiresAt();
        lastUsedAt = session.lastUsedAt();
        revokedAt = session.revokedAt();
        createdAt = session.createdAt();
    }

    AuthSession toDomain() {
        return AuthSession.restore(
                UUID.fromString(id),
                UserId.from(userId),
                accessTokenHash,
                refreshTokenHash,
                deviceName,
                accessExpiresAt,
                refreshExpiresAt,
                lastUsedAt,
                revokedAt,
                createdAt);
    }
}
