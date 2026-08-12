package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionStorePort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(UUID sessionId);

    Optional<AuthSession> findByAccessTokenHash(String tokenHash);

    default Optional<AuthenticatedSessionRecord> findAuthenticationByAccessTokenHash(
            String tokenHash) {
        return Optional.empty();
    }

    Optional<AuthSession> findByRefreshTokenHash(String tokenHash);

    List<AuthSession> findByUserId(UserId userId);

    record AuthenticatedSessionRecord(
            AuthSession session, String displayName, boolean accountCanSignIn) {}
}
