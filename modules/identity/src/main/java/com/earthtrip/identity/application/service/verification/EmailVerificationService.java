package com.earthtrip.identity.application.service.verification;

import com.earthtrip.identity.application.port.in.EmailVerificationUseCase;
import com.earthtrip.identity.application.port.out.AuthTokenStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.application.port.out.VerificationDeliveryPort;
import com.earthtrip.identity.domain.AuthToken;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class EmailVerificationService implements EmailVerificationUseCase {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserAccountStorePort accountStore;
    private final AuthTokenStorePort tokenStore;
    private final CredentialPort credentialPort;
    private final VerificationDeliveryPort deliveryPort;
    private final Clock clock;

    EmailVerificationService(
            UserAccountStorePort accountStore,
            AuthTokenStorePort tokenStore,
            CredentialPort credentialPort,
            VerificationDeliveryPort deliveryPort,
            Clock clock) {
        this.accountStore = accountStore;
        this.tokenStore = tokenStore;
        this.credentialPort = credentialPort;
        this.deliveryPort = deliveryPort;
        this.clock = clock;
    }

    @Override
    public RequestResult request(String rawEmail) {
        EmailAddress email = new EmailAddress(rawEmail);
        UserAccount account = accountStore.findByEmail(email).orElse(null);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(TOKEN_TTL);
        UUID requestId = UUID.randomUUID();
        if (account == null || account.emailVerifiedAt() != null) {
            return new RequestResult(requestId, expiresAt, "ACCEPTED");
        }

        tokenStore.invalidateFor(account.id(), AuthToken.Purpose.EMAIL_VERIFICATION, now);
        String rawToken = credentialPort.newToken();
        tokenStore.save(
                AuthToken.create(
                        requestId,
                        account.id(),
                        AuthToken.Purpose.EMAIL_VERIFICATION,
                        credentialPort.hashToken(rawToken),
                        expiresAt,
                        now));
        VerificationDeliveryPort.DeliveryStatus status =
                deliveryPort.sendEmailVerification(email, rawToken, expiresAt);
        return new RequestResult(requestId, expiresAt, status.name());
    }

    @Override
    public ConfirmResult confirm(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw EarthTripException.badRequest("VERIFICATION_TOKEN_REQUIRED", "이메일 인증 토큰이 필요합니다.");
        }
        AuthToken token =
                tokenStore
                        .findUsableByHash(
                                credentialPort.hashToken(rawToken),
                                AuthToken.Purpose.EMAIL_VERIFICATION)
                        .orElseThrow(
                                () ->
                                        EarthTripException.badRequest(
                                                "INVALID_VERIFICATION_TOKEN",
                                                "만료되었거나 올바르지 않은 인증 토큰입니다."));
        Instant now = clock.instant();
        try {
            token.consume(now);
        } catch (IllegalStateException exception) {
            throw EarthTripException.badRequest(
                    "INVALID_VERIFICATION_TOKEN", exception.getMessage());
        }
        UserAccount account =
                accountStore
                        .findById(token.userId())
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        account.verifyEmail(now);
        tokenStore.save(token);
        accountStore.save(account);
        return new ConfirmResult(account.id().value(), account.email().value(), now);
    }
}
