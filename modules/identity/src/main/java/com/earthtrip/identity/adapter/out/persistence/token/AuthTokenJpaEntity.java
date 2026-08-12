package com.earthtrip.identity.adapter.out.persistence.token;

import com.earthtrip.identity.domain.AuthToken;
import com.earthtrip.identity.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
class AuthTokenJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "purpose", nullable = false, length = 40)
    private String purpose;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthTokenJpaEntity() {}

    private AuthTokenJpaEntity(AuthToken token) {
        id = token.id().toString();
        apply(token);
    }

    static AuthTokenJpaEntity from(AuthToken token) {
        return new AuthTokenJpaEntity(token);
    }

    void apply(AuthToken token) {
        userId = token.userId().toString();
        purpose = token.purpose().name();
        tokenHash = token.tokenHash();
        expiresAt = token.expiresAt();
        consumedAt = token.consumedAt();
        createdAt = token.createdAt();
    }

    AuthToken toDomain() {
        return AuthToken.restore(
                java.util.UUID.fromString(id),
                UserId.from(userId),
                AuthToken.Purpose.valueOf(purpose),
                tokenHash,
                expiresAt,
                consumedAt,
                createdAt);
    }
}
