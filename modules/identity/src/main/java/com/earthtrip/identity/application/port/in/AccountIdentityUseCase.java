package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AccountIdentityUseCase {

    List<IdentityResult> list(UUID actorUserId);

    IdentityResult link(UUID actorUserId, String provider, OAuthCommand command);

    void unlink(UUID actorUserId, UUID identityId);

    SessionUseCase.SessionResult oauthSession(String provider, OAuthCommand command);

    EmailChangeRequestResult requestEmailChange(UUID actorUserId, String newEmail);

    EmailChangeResult confirmEmailChange(UUID actorUserId, String token);

    record OAuthCommand(
        String authorizationCode,
        String idToken,
        String redirectUri,
        String codeVerifier,
        String deviceName
    ) { }

    record IdentityResult(
        UUID identityId,
        String provider,
        String providerEmail,
        Instant createdAt,
        Instant lastUsedAt,
        long version
    ) { }

    record EmailChangeRequestResult(
        UUID requestId,
        String newEmail,
        Instant expiresAt,
        String deliveryStatus
    ) { }

    record EmailChangeResult(UUID userId, String email, Instant confirmedAt) { }
}
