package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.AuthToken;
import com.earthtrip.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;

public interface AuthTokenStorePort {

    AuthToken save(AuthToken token);

    Optional<AuthToken> findUsableByHash(String tokenHash, AuthToken.Purpose purpose);

    void invalidateFor(UserId userId, AuthToken.Purpose purpose, Instant now);
}
