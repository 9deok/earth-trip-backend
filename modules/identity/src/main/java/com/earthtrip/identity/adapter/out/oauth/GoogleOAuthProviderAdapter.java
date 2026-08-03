package com.earthtrip.identity.adapter.out.oauth;

import com.earthtrip.identity.application.port.out.OAuthProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String GOOGLE_JWK_SET_URI =
        "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
        "https://accounts.google.com",
        "accounts.google.com"
    );

    private final Set<String> clientIds;
    private final JwtDecoder decoder;

    GoogleOAuthProviderAdapter(
        @Value("${earthtrip.oauth.google.client-ids:}") String configuredClientIds
    ) {
        this(parseClientIds(configuredClientIds), googleJwtDecoder());
    }

    GoogleOAuthProviderAdapter(Set<String> clientIds, JwtDecoder decoder) {
        this.clientIds = Set.copyOf(clientIds);
        this.decoder = decoder;
    }

    @Override
    public VerifiedIdentity verify(String provider, OAuthCredential credential) {
        String normalizedProvider = provider == null
            ? ""
            : provider.strip().toUpperCase(Locale.ROOT);
        if (!GOOGLE_PROVIDER.equals(normalizedProvider)) {
            throw EarthTripException.unavailable(
                "OAUTH_PROVIDER_NOT_CONFIGURED",
                normalizedProvider + " OAuth 제공자 자격증명이 아직 설정되지 않았습니다."
            );
        }
        if (clientIds.isEmpty()) {
            throw EarthTripException.unavailable(
                "GOOGLE_OAUTH_NOT_CONFIGURED",
                "Google OAuth 웹 클라이언트 ID가 설정되지 않았습니다."
            );
        }
        if (credential == null || credential.idToken() == null
            || credential.idToken().isBlank()) {
            throw EarthTripException.badRequest(
                "GOOGLE_ID_TOKEN_REQUIRED",
                "Google ID 토큰이 필요합니다."
            );
        }

        Jwt token;
        try {
            token = decoder.decode(credential.idToken());
        } catch (JwtDecoderInitializationException exception) {
            throw providerUnavailable();
        } catch (JwtException exception) {
            if (hasRestClientCause(exception)) {
                throw providerUnavailable();
            }
            throw invalidToken();
        }
        validateClaims(token);

        return new VerifiedIdentity(
            token.getSubject(),
            token.getClaimAsString("email"),
            Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified")),
            token.getClaimAsString("name")
        );
    }

    private void validateClaims(Jwt token) {
        String issuer = token.getIssuer() == null ? null : token.getIssuer().toString();
        if (!GOOGLE_ISSUERS.contains(issuer)) {
            throw invalidToken();
        }
        if (token.getAudience().stream().noneMatch(clientIds::contains)) {
            throw invalidToken();
        }
        if (token.getSubject() == null || token.getSubject().isBlank()) {
            throw invalidToken();
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

    private static JwtDecoder googleJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(GOOGLE_JWK_SET_URI)
            .build();
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

    private static EarthTripException invalidToken() {
        return EarthTripException.unauthorized(
            "INVALID_GOOGLE_ID_TOKEN",
            "Google 로그인 정보가 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요."
        );
    }

    private static EarthTripException providerUnavailable() {
        return EarthTripException.unavailable(
            "GOOGLE_IDENTITY_PROVIDER_UNAVAILABLE",
            "Google 로그인 검증 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
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
