package com.earthtrip.identity.application.service.password;

import com.earthtrip.identity.application.port.in.PasswordResetUseCase;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
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
class PasswordResetService implements PasswordResetUseCase {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserAccountStorePort accountStore;
    private final AuthTokenStorePort tokenStore;
    private final AuthSessionStorePort sessionStore;
    private final CredentialPort credentialPort;
    private final VerificationDeliveryPort deliveryPort;
    private final Clock clock;

    PasswordResetService(
            UserAccountStorePort accountStore,
            AuthTokenStorePort tokenStore,
            AuthSessionStorePort sessionStore,
            CredentialPort credentialPort,
            VerificationDeliveryPort deliveryPort,
            Clock clock) {
        this.accountStore = accountStore;
        this.tokenStore = tokenStore;
        this.sessionStore = sessionStore;
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
        if (account == null || account.status() == UserAccount.Status.DELETED) {
            return new RequestResult(requestId, expiresAt, "ACCEPTED");
        }
        tokenStore.invalidateFor(account.id(), AuthToken.Purpose.PASSWORD_RESET, now);
        String rawToken = credentialPort.newToken();
        tokenStore.save(
                AuthToken.create(
                        requestId,
                        account.id(),
                        AuthToken.Purpose.PASSWORD_RESET,
                        credentialPort.hashToken(rawToken),
                        expiresAt,
                        now));
        VerificationDeliveryPort.DeliveryStatus delivery =
                deliveryPort.sendPasswordReset(email, rawToken, expiresAt);
        return new RequestResult(requestId, expiresAt, delivery.name());
    }

    @Override
    public void reset(String rawToken, String newPassword) {
        validatePassword(newPassword);
        if (rawToken == null || rawToken.isBlank()) {
            throw EarthTripException.badRequest("RESET_TOKEN_REQUIRED", "비밀번호 재설정 토큰이 필요합니다.");
        }
        AuthToken token =
                tokenStore
                        .findUsableByHash(
                                credentialPort.hashToken(rawToken),
                                AuthToken.Purpose.PASSWORD_RESET)
                        .orElseThrow(
                                () ->
                                        EarthTripException.badRequest(
                                                "INVALID_RESET_TOKEN",
                                                "만료되었거나 올바르지 않은 재설정 토큰입니다."));
        Instant now = clock.instant();
        try {
            token.consume(now);
        } catch (IllegalStateException exception) {
            throw EarthTripException.badRequest("INVALID_RESET_TOKEN", exception.getMessage());
        }
        UserAccount account =
                accountStore
                        .findById(token.userId())
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        account.changePassword(credentialPort.hashPassword(newPassword), now);
        tokenStore.save(token);
        accountStore.save(account);
        sessionStore
                .findByUserId(account.id())
                .forEach(
                        session -> {
                            session.revoke(now);
                            sessionStore.save(session);
                        });
    }

    private static void validatePassword(String password) {
        if (password == null
                || password.length() < 10
                || password.length() > 128
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw EarthTripException.badRequest(
                    "WEAK_PASSWORD", "비밀번호는 10~128자이며 문자와 숫자를 포함해야 합니다.");
        }
    }
}
