package com.earthtrip.identity.adapter.out.oauth;

import com.earthtrip.identity.application.port.out.OAuthProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
class GoogleOAuthProviderAdapter implements OAuthProviderPort {

    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final String APPLE_PROVIDER = "APPLE";
    private static final String GOOGLE_JWK_SET_URI =
        "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
        "https://accounts.google.com",
        "accounts.google.com"
    );
    private static final String APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final Set<String> clientIds;
    private final Set<String> appleClientIds;
    private final JwtDecoder googleDecoder;
    private final JwtDecoder appleDecoder;

    @Autowired
    GoogleOAuthProviderAdapter(
        @Value("${earthtrip.oauth.google.client-ids:}") String configuredClientIds,
        @Value("${earthtrip.oauth.apple.client-ids:}") String configuredAppleClientIds
    ) {
        this(
            parseClientIds(configuredClientIds), googleJwtDecoder(),
            parseClientIds(configuredAppleClientIds), appleJwtDecoder()
        );
    }

    GoogleOAuthProviderAdapter(Set<String> clientIds, JwtDecoder decoder) {
        this(clientIds, decoder, Set.of(), ignored -> {
            throw new JwtException("Apple decoder is not configured in this test.");
        });
    }

    GoogleOAuthProviderAdapter(
        Set<String> clientIds,
        JwtDecoder googleDecoder,
        Set<String> appleClientIds,
        JwtDecoder appleDecoder
    ) {
        this.clientIds = Set.copyOf(clientIds);
        this.googleDecoder = googleDecoder;
        this.appleClientIds = Set.copyOf(appleClientIds);
        this.appleDecoder = appleDecoder;
    }

    @Override
    public VerifiedIdentity verify(String provider, OAuthCredential credential) {
        String normalizedProvider = provider == null
            ? ""
            : provider.strip().toUpperCase(Locale.ROOT);
        if (GOOGLE_PROVIDER.equals(normalizedProvider)) {
            return verifyGoogle(credential);
        }
        if (APPLE_PROVIDER.equals(normalizedProvider)) {
            return verifyApple(credential);
        }
        throw EarthTripException.badRequest(
            "INVALID_OAUTH_PROVIDER",
            "지원하지 않는 OAuth 제공자입니다."
        );
    }

    private VerifiedIdentity verifyGoogle(OAuthCredential credential) {
        if (clientIds.isEmpty()) {
            throw EarthTripException.unavailable(
                "GOOGLE_OAUTH_NOT_CONFIGURED",
                "Google OAuth 웹 클라이언트 ID가 설정되지 않았습니다."
            );
        }
        requireIdToken(credential, "GOOGLE_ID_TOKEN_REQUIRED", "Google ID 토큰이 필요합니다.");
        Jwt token = decode(googleDecoder, credential.idToken(), true);
        validateGoogleClaims(token);
        return new VerifiedIdentity(
            token.getSubject(), token.getClaimAsString("email"), true,
            token.getClaimAsString("name")
        );
    }

    private VerifiedIdentity verifyApple(OAuthCredential credential) {
        if (appleClientIds.isEmpty()) {
            throw EarthTripException.unavailable(
                "APPLE_OAUTH_NOT_CONFIGURED",
                "Apple Sign in 서비스 ID 또는 앱 번들 ID가 설정되지 않았습니다."
            );
        }
        requireIdToken(credential, "APPLE_ID_TOKEN_REQUIRED", "Apple ID 토큰이 필요합니다.");
        Jwt token = decode(appleDecoder, credential.idToken(), false);
        validateAppleClaims(token);
        return new VerifiedIdentity(
            token.getSubject(), token.getClaimAsString("email"), true, null
        );
    }

    private static Jwt decode(JwtDecoder decoder, String rawToken, boolean google) {
        try {
            return decoder.decode(rawToken);
        } catch (JwtDecoderInitializationException exception) {
            throw providerUnavailable(google);
        } catch (JwtException exception) {
            if (hasRestClientCause(exception)) {
                throw providerUnavailable(google);
            }
            throw invalidToken(google);
        }
    }

    private void validateGoogleClaims(Jwt token) {
        String issuer = token.getIssuer() == null ? null : token.getIssuer().toString();
        if (!GOOGLE_ISSUERS.contains(issuer)) {
            throw invalidToken(true);
        }
        if (token.getAudience().stream().noneMatch(clientIds::contains)) {
            throw invalidToken(true);
        }
        if (token.getSubject() == null || token.getSubject().isBlank()) {
            throw invalidToken(true);
        }
        String email = token.getClaimAsString("email");
        if (email == null || email.isBlank()
            || !Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))) {
            throw EarthTripException.forbidden(
                "VERIFIED_EMAIL_REQUIRED",
                "인증된 이메일을 제공하는 Google 계정만 사용할 수 있습니다."
            );
        }
    }

    private void validateAppleClaims(Jwt token) {
        String issuer = token.getIssuer() == null ? null : token.getIssuer().toString();
        if (!APPLE_ISSUER.equals(issuer)
            || token.getAudience().stream().noneMatch(appleClientIds::contains)
            || token.getSubject() == null || token.getSubject().isBlank()) {
            throw invalidToken(false);
        }
        String email = token.getClaimAsString("email");
        Object verified = token.getClaim("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified)
            || "true".equalsIgnoreCase(String.valueOf(verified));
        if (email == null || email.isBlank() || !emailVerified) {
            throw EarthTripException.forbidden(
                "VERIFIED_EMAIL_REQUIRED",
                "인증된 이메일을 제공하는 Apple 계정만 사용할 수 있습니다."
            );
        }
    }

    private static void requireIdToken(
        OAuthCredential credential,
        String code,
        String message
    ) {
        if (credential == null || credential.idToken() == null
            || credential.idToken().isBlank()) {
            throw EarthTripException.badRequest(code, message);
        }
    }

    private static JwtDecoder googleJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(GOOGLE_JWK_SET_URI)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    private static JwtDecoder appleJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWK_SET_URI).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    private static Set<String> parseClientIds(String configuredClientIds) {
        if (configuredClientIds == null || configuredClientIds.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredClientIds.split(","))
            .map(String::strip)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static EarthTripException invalidToken(boolean google) {
        return EarthTripException.unauthorized(
            google ? "INVALID_GOOGLE_ID_TOKEN" : "INVALID_APPLE_ID_TOKEN",
            (google ? "Google" : "Apple")
                + " 로그인 정보가 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요."
        );
    }

    private static EarthTripException providerUnavailable(boolean google) {
        return EarthTripException.unavailable(
            google
                ? "GOOGLE_IDENTITY_PROVIDER_UNAVAILABLE"
                : "APPLE_IDENTITY_PROVIDER_UNAVAILABLE",
            (google ? "Google" : "Apple")
                + " 로그인 검증 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
        );
    }

    private static boolean hasRestClientCause(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RestClientException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
