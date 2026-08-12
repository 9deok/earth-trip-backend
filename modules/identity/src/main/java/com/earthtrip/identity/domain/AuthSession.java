package com.earthtrip.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthSession {

    private final UUID id;
    private final UserId userId;
    private String accessTokenHash;
    private String refreshTokenHash;
    private final String deviceName;
    private Instant accessExpiresAt;
    private Instant refreshExpiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;
    private final Instant createdAt;

    private AuthSession(
            UUID id,
            UserId userId,
            String accessTokenHash,
            String refreshTokenHash,
            String deviceName,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant lastUsedAt,
            Instant revokedAt,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.accessTokenHash = Objects.requireNonNull(accessTokenHash);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.deviceName = normalizeDevice(deviceName);
        this.accessExpiresAt = Objects.requireNonNull(accessExpiresAt);
        this.refreshExpiresAt = Objects.requireNonNull(refreshExpiresAt);
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt);
        this.revokedAt = revokedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AuthSession create(
            UUID id,
            UserId userId,
            String accessTokenHash,
            String refreshTokenHash,
            String deviceName,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant now) {
        return new AuthSession(
                id,
                userId,
                accessTokenHash,
                refreshTokenHash,
                deviceName,
                accessExpiresAt,
                refreshExpiresAt,
                now,
                null,
                now);
    }

    public static AuthSession restore(
            UUID id,
            UserId userId,
            String accessTokenHash,
            String refreshTokenHash,
            String deviceName,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant lastUsedAt,
            Instant revokedAt,
            Instant createdAt) {
        return new AuthSession(
                id,
                userId,
                accessTokenHash,
                refreshTokenHash,
                deviceName,
                accessExpiresAt,
                refreshExpiresAt,
                lastUsedAt,
                revokedAt,
                createdAt);
    }

    public boolean acceptsAccessAt(Instant now) {
        return revokedAt == null && now.isBefore(accessExpiresAt);
    }

    public boolean acceptsRefreshAt(Instant now) {
        return revokedAt == null && now.isBefore(refreshExpiresAt);
    }

    public void rotate(
            String newAccessTokenHash,
            String newRefreshTokenHash,
            Instant newAccessExpiry,
            Instant newRefreshExpiry,
            Instant now) {
        if (!acceptsRefreshAt(now)) {
            throw new IllegalStateException("갱신할 수 없는 세션입니다.");
        }
        accessTokenHash = Objects.requireNonNull(newAccessTokenHash);
        refreshTokenHash = Objects.requireNonNull(newRefreshTokenHash);
        accessExpiresAt = Objects.requireNonNull(newAccessExpiry);
        refreshExpiresAt = Objects.requireNonNull(newRefreshExpiry);
        lastUsedAt = Objects.requireNonNull(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = Objects.requireNonNull(now);
    }

    public UUID id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String accessTokenHash() {
        return accessTokenHash;
    }

    public String refreshTokenHash() {
        return refreshTokenHash;
    }

    public String deviceName() {
        return deviceName;
    }

    public Instant accessExpiresAt() {
        return accessExpiresAt;
    }

    public Instant refreshExpiresAt() {
        return refreshExpiresAt;
    }

    public Instant lastUsedAt() {
        return lastUsedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String normalizeDevice(String value) {
        if (value == null || value.isBlank()) return "Unknown device";
        String normalized = value.strip();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }
}
