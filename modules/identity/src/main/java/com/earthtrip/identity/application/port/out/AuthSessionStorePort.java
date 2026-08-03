package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.AuthSession;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.earthtrip.identity.domain.UserId;

public interface AuthSessionStorePort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(UUID sessionId);

    Optional<AuthSession> findByAccessTokenHash(String tokenHash);

    Optional<AuthSession> findByRefreshTokenHash(String tokenHash);

    List<AuthSession> findByUserId(UserId userId);
}
