package com.earthtrip.identity.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.identity.application.port.out.OAuthProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;

class GoogleOAuthProviderAdapterTest {

    private static final String CLIENT_ID = "earth-trip.apps.googleusercontent.com";
    private static final String APPLE_CLIENT_ID = "com.earthtrip.app";

    @Test
    void verifiesSignedGoogleIdentityClaimsForConfiguredAudience() {
        GoogleOAuthProviderAdapter adapter = adapter(validToken());

        OAuthProviderPort.VerifiedIdentity identity = adapter.verify(
            "google",
            credential("signed-google-token")
        );

        assertThat(identity.subject()).isEqualTo("google-subject-123");
        assertThat(identity.email()).isEqualTo("traveler@example.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.displayName()).isEqualTo("여행자");
    }

    @Test
    void rejectsTokenForAnotherAudience() {
        Jwt token = tokenBuilder()
            .audience(List.of("another-client.apps.googleusercontent.com"))
            .claim("email", "traveler@example.com")
            .claim("email_verified", true)
            .build();

        assertThatThrownBy(() -> adapter(token).verify("GOOGLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("INVALID_GOOGLE_ID_TOKEN");
                assertThat(error.httpStatus()).isEqualTo(401);
            });
    }

    @Test
    void rejectsUnverifiedEmail() {
        Jwt token = tokenBuilder()
            .audience(List.of(CLIENT_ID))
            .claim("email", "traveler@example.com")
            .claim("email_verified", false)
            .build();

        assertThatThrownBy(() -> adapter(token).verify("GOOGLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("VERIFIED_EMAIL_REQUIRED");
                assertThat(error.httpStatus()).isEqualTo(403);
            });
    }

    @Test
    void failsClosedUntilClientIdIsConfigured() {
        GoogleOAuthProviderAdapter adapter = new GoogleOAuthProviderAdapter(
            Set.of(),
            ignored -> validToken()
        );

        assertThatThrownBy(() -> adapter.verify("GOOGLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("GOOGLE_OAUTH_NOT_CONFIGURED");
                assertThat(error.httpStatus()).isEqualTo(503);
            });
    }

    @Test
    void mapsSignatureOrTimestampFailureToUnauthorized() {
        JwtDecoder rejectingDecoder = ignored -> {
            throw new BadJwtException("invalid signature");
        };
        GoogleOAuthProviderAdapter adapter = new GoogleOAuthProviderAdapter(
            Set.of(CLIENT_ID),
            rejectingDecoder
        );

        assertThatThrownBy(() -> adapter.verify("GOOGLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("INVALID_GOOGLE_ID_TOKEN");
                assertThat(error.httpStatus()).isEqualTo(401);
            });
    }

    @Test
    void mapsGoogleKeyDiscoveryFailureToServiceUnavailable() {
        JwtDecoder unavailableDecoder = ignored -> {
            throw new JwtDecoderInitializationException(
                "Google key discovery failed",
                new IllegalStateException("network unavailable")
            );
        };
        GoogleOAuthProviderAdapter adapter = new GoogleOAuthProviderAdapter(
            Set.of(CLIENT_ID),
            unavailableDecoder
        );

        assertThatThrownBy(() -> adapter.verify("GOOGLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("GOOGLE_IDENTITY_PROVIDER_UNAVAILABLE");
                assertThat(error.httpStatus()).isEqualTo(503);
            });
    }

    @Test
    void verifiesSignedAppleIdentityClaimsForConfiguredAudience() {
        GoogleOAuthProviderAdapter adapter = appleAdapter(validAppleToken());

        OAuthProviderPort.VerifiedIdentity identity = adapter.verify(
            "apple",
            credential("signed-apple-token")
        );

        assertThat(identity.subject()).isEqualTo("apple-subject-123");
        assertThat(identity.email()).isEqualTo("apple-traveler@example.com");
        assertThat(identity.emailVerified()).isTrue();
    }

    @Test
    void rejectsAppleTokenForAnotherAudience() {
        Jwt token = appleTokenBuilder()
            .audience(List.of("another.bundle.id"))
            .claim("email", "apple-traveler@example.com")
            .claim("email_verified", "true")
            .build();

        assertThatThrownBy(() -> appleAdapter(token).verify("APPLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("INVALID_APPLE_ID_TOKEN");
                assertThat(error.httpStatus()).isEqualTo(401);
            });
    }

    @Test
    void failsClosedUntilAppleClientIdIsConfigured() {
        GoogleOAuthProviderAdapter adapter = new GoogleOAuthProviderAdapter(
            Set.of(CLIENT_ID), ignored -> validToken(), Set.of(), ignored -> validAppleToken()
        );

        assertThatThrownBy(() -> adapter.verify("APPLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("APPLE_OAUTH_NOT_CONFIGURED");
                assertThat(error.httpStatus()).isEqualTo(503);
            });
    }

    @Test
    void mapsAppleKeyDiscoveryFailureToServiceUnavailable() {
        JwtDecoder unavailableDecoder = ignored -> {
            throw new JwtDecoderInitializationException(
                "Apple key discovery failed",
                new IllegalStateException("network unavailable")
            );
        };
        GoogleOAuthProviderAdapter adapter = new GoogleOAuthProviderAdapter(
            Set.of(CLIENT_ID), ignored -> validToken(),
            Set.of(APPLE_CLIENT_ID), unavailableDecoder
        );

        assertThatThrownBy(() -> adapter.verify("APPLE", credential("token")))
            .isInstanceOfSatisfying(EarthTripException.class, error -> {
                assertThat(error.code()).isEqualTo("APPLE_IDENTITY_PROVIDER_UNAVAILABLE");
                assertThat(error.httpStatus()).isEqualTo(503);
            });
    }

    private static GoogleOAuthProviderAdapter adapter(Jwt token) {
        return new GoogleOAuthProviderAdapter(Set.of(CLIENT_ID), ignored -> token);
    }

    private static GoogleOAuthProviderAdapter appleAdapter(Jwt token) {
        return new GoogleOAuthProviderAdapter(
            Set.of(CLIENT_ID), ignored -> validToken(),
            Set.of(APPLE_CLIENT_ID), ignored -> token
        );
    }

    private static OAuthProviderPort.OAuthCredential credential(String idToken) {
        return new OAuthProviderPort.OAuthCredential(null, idToken, null, null);
    }

    private static Jwt validToken() {
        return tokenBuilder()
            .audience(List.of(CLIENT_ID))
            .claim("email", "traveler@example.com")
            .claim("email_verified", true)
            .claim("name", "여행자")
            .build();
    }

    private static Jwt validAppleToken() {
        return appleTokenBuilder()
            .audience(List.of(APPLE_CLIENT_ID))
            .claim("email", "apple-traveler@example.com")
            .claim("email_verified", "true")
            .build();
    }

    private static Jwt.Builder tokenBuilder() {
        Instant now = Instant.parse("2026-08-02T00:00:00Z");
        return Jwt.withTokenValue("signed-google-token")
            .header("alg", "RS256")
            .issuer("https://accounts.google.com")
            .subject("google-subject-123")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600));
    }

    private static Jwt.Builder appleTokenBuilder() {
        Instant now = Instant.parse("2026-08-02T00:00:00Z");
        return Jwt.withTokenValue("signed-apple-token")
            .header("alg", "RS256")
            .issuer("https://appleid.apple.com")
            .subject("apple-subject-123")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600));
    }
}
