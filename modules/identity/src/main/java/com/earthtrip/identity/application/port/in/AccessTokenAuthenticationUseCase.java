package com.earthtrip.identity.application.port.in;

import java.util.UUID;

public interface AccessTokenAuthenticationUseCase {

    AuthenticationResult authenticate(String rawAccessToken);

    record AuthenticationResult(UUID userId, UUID sessionId, String displayName) { }
}
