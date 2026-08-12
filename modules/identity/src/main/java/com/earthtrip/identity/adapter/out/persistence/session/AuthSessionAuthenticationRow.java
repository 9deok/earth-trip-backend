package com.earthtrip.identity.adapter.out.persistence.session;

import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserId;
import java.time.Instant;
import java.util.UUID;

public record AuthSessionAuthenticationRow(
        String id,
        String userId,
        String accessTokenHash,
        String refreshTokenHash,
        String deviceName,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        Instant lastUsedAt,
        Instant revokedAt,
        Instant createdAt,
        String displayName,
        String accountStatus) {

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

    boolean accountCanSignIn() {
        return "ACTIVE".equals(accountStatus) || "DELETION_PENDING".equals(accountStatus);
    }
}
