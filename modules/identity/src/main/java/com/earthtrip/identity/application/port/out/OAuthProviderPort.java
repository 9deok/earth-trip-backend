package com.earthtrip.identity.application.port.out;

public interface OAuthProviderPort {

    VerifiedIdentity verify(String provider, OAuthCredential credential);

    record OAuthCredential(
        String authorizationCode,
        String idToken,
        String redirectUri,
        String codeVerifier
    ) { }

    record VerifiedIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String displayName
    ) { }
}
