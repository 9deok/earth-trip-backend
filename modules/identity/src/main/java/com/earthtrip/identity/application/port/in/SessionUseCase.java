package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionUseCase {

    SessionResult create(String email, String password, String deviceName);

    SessionResult refresh(String refreshToken);

    void revoke(UUID sessionId, UUID actorUserId);

    List<DeviceSessionResult> list(UUID userId, UUID currentSessionId);

    void revokeOtherSessions(UUID userId, UUID currentSessionId, boolean includeCurrent);

    record DeviceSessionResult(
            UUID sessionId,
            String deviceName,
            boolean current,
            boolean active,
            Instant lastUsedAt,
            Instant createdAt) {}

    record SessionResult(
            UUID sessionId,
            UUID userId,
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {}
}
