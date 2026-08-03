package com.earthtrip.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthToken {

    public enum Purpose { EMAIL_VERIFICATION, PASSWORD_RESET }

    private final UUID id;
    private final UserId userId;
    private final Purpose purpose;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant consumedAt;
    private final Instant createdAt;

    private AuthToken(
        UUID id,
        UserId userId,
        Purpose purpose,
        String tokenHash,
        Instant expiresAt,
        Instant consumedAt,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.purpose = Objects.requireNonNull(purpose);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.consumedAt = consumedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AuthToken create(
        UUID id,
        UserId userId,
        Purpose purpose,
        String tokenHash,
        Instant expiresAt,
        Instant now
    ) {
        return new AuthToken(id, userId, purpose, tokenHash, expiresAt, null, now);
    }

    public static AuthToken restore(
        UUID id,
        UserId userId,
        Purpose purpose,
        String tokenHash,
        Instant expiresAt,
        Instant consumedAt,
        Instant createdAt
    ) {
        return new AuthToken(id, userId, purpose, tokenHash, expiresAt, consumedAt, createdAt);
    }

    public void consume(Instant now) {
        if (consumedAt != null || !now.isBefore(expiresAt)) {
            throw new IllegalStateException("만료되었거나 이미 사용한 토큰입니다.");
        }
        consumedAt = now;
    }

    public UUID id() { return id; }

    public UserId userId() { return userId; }

    public Purpose purpose() { return purpose; }

    public String tokenHash() { return tokenHash; }

    public Instant expiresAt() { return expiresAt; }

    public Instant consumedAt() { return consumedAt; }

    public Instant createdAt() { return createdAt; }
}
