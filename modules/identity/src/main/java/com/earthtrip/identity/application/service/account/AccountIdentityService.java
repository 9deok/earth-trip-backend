package com.earthtrip.identity.application.service.account;

import com.earthtrip.identity.application.port.in.AccountIdentityUseCase;
import com.earthtrip.identity.application.port.in.SessionUseCase;
import com.earthtrip.identity.application.port.out.AccountIdentityStorePort;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.OAuthProviderPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.application.port.out.VerificationDeliveryPort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AccountIdentityService implements AccountIdentityUseCase {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final Duration EMAIL_CHANGE_TTL = Duration.ofMinutes(30);

    private final AccountIdentityStorePort identities;
    private final OAuthProviderPort oauth;
    private final UserAccountStorePort accounts;
    private final AuthSessionStorePort sessions;
    private final CredentialPort credentials;
    private final VerificationDeliveryPort delivery;
    private final Clock clock;

    AccountIdentityService(
            AccountIdentityStorePort identities,
            OAuthProviderPort oauth,
            UserAccountStorePort accounts,
            AuthSessionStorePort sessions,
            CredentialPort credentials,
            VerificationDeliveryPort delivery,
            Clock clock) {
        this.identities = identities;
        this.oauth = oauth;
        this.accounts = accounts;
        this.sessions = sessions;
        this.credentials = credentials;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityResult> list(UUID actorUserId) {
        loadAccount(actorUserId);
        return identities.findByUser(actorUserId).stream()
                .map(AccountIdentityService::result)
                .toList();
    }

    @Override
    public IdentityResult link(UUID actorUserId, String provider, OAuthCommand command) {
        loadAccount(actorUserId);
        String normalized = provider(provider);
        OAuthProviderPort.VerifiedIdentity verified = verify(normalized, command);
        AccountIdentityStorePort.IdentityRecord existing =
                identities.findIdentity(normalized, verified.subject()).orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(actorUserId)) {
                throw EarthTripException.conflict(
                        "OAUTH_IDENTITY_ALREADY_LINKED", "이미 다른 계정에 연결된 로그인 수단입니다.");
            }
            return result(touch(existing, verified.email()));
        }
        Instant now = clock.instant();
        return result(
                identities.saveIdentity(
                        new AccountIdentityStorePort.IdentityRecord(
                                UUID.randomUUID(),
                                actorUserId,
                                normalized,
                                verified.subject(),
                                verified.email(),
                                now,
                                now,
                                0)));
    }

    @Override
    public void unlink(UUID actorUserId, UUID identityId) {
        loadAccount(actorUserId);
        AccountIdentityStorePort.IdentityRecord identity =
                identities
                        .findIdentity(identityId)
                        .filter(item -> item.userId().equals(actorUserId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "LINKED_IDENTITY_NOT_FOUND",
                                                "연결된 로그인 수단을 찾을 수 없습니다."));
        if (identities.findByUser(actorUserId).size() <= 1) {
            throw EarthTripException.conflict("LAST_LOGIN_IDENTITY", "마지막 외부 로그인 수단은 해제할 수 없습니다.");
        }
        identities.deleteIdentity(identity.id());
    }

    @Override
    public SessionUseCase.SessionResult oauthSession(String provider, OAuthCommand command) {
        String normalized = provider(provider);
        OAuthProviderPort.VerifiedIdentity verified = verify(normalized, command);
        AccountIdentityStorePort.IdentityRecord identity =
                identities.findIdentity(normalized, verified.subject()).orElse(null);
        UserAccount account;
        if (identity == null) {
            account = createOAuthAccount(verified);
            Instant now = clock.instant();
            identity =
                    identities.saveIdentity(
                            new AccountIdentityStorePort.IdentityRecord(
                                    UUID.randomUUID(),
                                    account.id().value(),
                                    normalized,
                                    verified.subject(),
                                    verified.email(),
                                    now,
                                    now,
                                    0));
        } else {
            account = loadAccount(identity.userId());
            touch(identity, verified.email());
        }
        if (!account.canSignIn()) {
            throw EarthTripException.forbidden("ACCOUNT_UNAVAILABLE", "현재 이 계정으로 로그인할 수 없습니다.");
        }
        return issueSession(account, command == null ? null : command.deviceName());
    }

    @Override
    public EmailChangeRequestResult requestEmailChange(UUID actorUserId, String rawEmail) {
        UserAccount account = loadAccount(actorUserId);
        EmailAddress newEmail = new EmailAddress(rawEmail);
        if (newEmail.equals(account.email())) {
            throw EarthTripException.badRequest("EMAIL_UNCHANGED", "현재 이메일과 다른 이메일을 입력해 주세요.");
        }
        if (accounts.findByEmail(newEmail).isPresent()) {
            throw EarthTripException.conflict("EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다.");
        }
        Instant now = clock.instant();
        identities
                .findPendingEmailChange(actorUserId)
                .ifPresent(
                        previous ->
                                identities.saveEmailChange(
                                        new AccountIdentityStorePort.EmailChangeRecord(
                                                previous.id(),
                                                previous.userId(),
                                                previous.newEmail(),
                                                previous.tokenHash(),
                                                "SUPERSEDED",
                                                previous.expiresAt(),
                                                previous.createdAt(),
                                                null)));
        String rawToken = credentials.newToken();
        Instant expiresAt = now.plus(EMAIL_CHANGE_TTL);
        AccountIdentityStorePort.EmailChangeRecord saved =
                identities.saveEmailChange(
                        new AccountIdentityStorePort.EmailChangeRecord(
                                UUID.randomUUID(),
                                actorUserId,
                                newEmail.value(),
                                credentials.hashToken(rawToken),
                                "PENDING",
                                expiresAt,
                                now,
                                null));
        VerificationDeliveryPort.DeliveryStatus status =
                delivery.sendEmailChange(newEmail, rawToken, expiresAt);
        return new EmailChangeRequestResult(
                saved.id(), saved.newEmail(), saved.expiresAt(), status.name());
    }

    @Override
    public EmailChangeResult confirmEmailChange(UUID actorUserId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw EarthTripException.badRequest("EMAIL_CHANGE_TOKEN_REQUIRED", "이메일 변경 토큰이 필요합니다.");
        }
        AccountIdentityStorePort.EmailChangeRecord request =
                identities
                        .findEmailChangeByTokenHash(credentials.hashToken(rawToken))
                        .filter(item -> item.userId().equals(actorUserId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.badRequest(
                                                "INVALID_EMAIL_CHANGE_TOKEN",
                                                "만료되었거나 올바르지 않은 이메일 변경 토큰입니다."));
        Instant now = clock.instant();
        if (!request.status().equals("PENDING") || !request.expiresAt().isAfter(now)) {
            throw EarthTripException.badRequest(
                    "INVALID_EMAIL_CHANGE_TOKEN", "만료되었거나 이미 사용된 이메일 변경 토큰입니다.");
        }
        EmailAddress newEmail = new EmailAddress(request.newEmail());
        accounts.findByEmail(newEmail)
                .filter(other -> !other.id().value().equals(actorUserId))
                .ifPresent(
                        other -> {
                            throw EarthTripException.conflict(
                                    "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다.");
                        });
        UserAccount account = loadAccount(actorUserId);
        account.changeEmail(newEmail, now);
        accounts.save(account);
        identities.saveEmailChange(
                new AccountIdentityStorePort.EmailChangeRecord(
                        request.id(),
                        request.userId(),
                        request.newEmail(),
                        request.tokenHash(),
                        "CONFIRMED",
                        request.expiresAt(),
                        request.createdAt(),
                        now));
        return new EmailChangeResult(actorUserId, newEmail.value(), now);
    }

    private OAuthProviderPort.VerifiedIdentity verify(String provider, OAuthCommand command) {
        if (command == null) {
            throw EarthTripException.badRequest("OAUTH_CREDENTIAL_REQUIRED", "OAuth 인증 결과가 필요합니다.");
        }
        return oauth.verify(
                provider,
                new OAuthProviderPort.OAuthCredential(
                        command.authorizationCode(),
                        command.idToken(),
                        command.redirectUri(),
                        command.codeVerifier()));
    }

    private UserAccount createOAuthAccount(OAuthProviderPort.VerifiedIdentity verified) {
        if (!verified.emailVerified() || verified.email() == null) {
            throw EarthTripException.forbidden(
                    "VERIFIED_EMAIL_REQUIRED", "인증된 이메일을 제공하는 OAuth 계정만 사용할 수 있습니다.");
        }
        EmailAddress email = new EmailAddress(verified.email());
        if (accounts.findByEmail(email).isPresent()) {
            throw EarthTripException.conflict(
                    "OAUTH_LINK_REQUIRED", "같은 이메일 계정에 로그인한 뒤 외부 로그인을 연결해 주세요.");
        }
        Instant now = clock.instant();
        String displayName =
                verified.displayName() == null || verified.displayName().isBlank()
                        ? email.value().substring(0, email.value().indexOf('@'))
                        : verified.displayName();
        UserAccount account =
                UserAccount.register(
                        new UserId(UUID.randomUUID()),
                        email,
                        credentials.hashPassword(credentials.newToken()),
                        displayName,
                        now);
        account.verifyEmail(now);
        return accounts.save(account);
    }

    private SessionUseCase.SessionResult issueSession(UserAccount account, String rawDeviceName) {
        Instant now = clock.instant();
        String accessToken = credentials.newToken();
        String refreshToken = credentials.newToken();
        AuthSession session =
                AuthSession.create(
                        UUID.randomUUID(),
                        account.id(),
                        credentials.hashToken(accessToken),
                        credentials.hashToken(refreshToken),
                        rawDeviceName == null || rawDeviceName.isBlank()
                                ? "OAuth device"
                                : rawDeviceName,
                        now.plus(ACCESS_TTL),
                        now.plus(REFRESH_TTL),
                        now);
        sessions.save(session);
        return new SessionUseCase.SessionResult(
                session.id(),
                account.id().value(),
                accessToken,
                refreshToken,
                session.accessExpiresAt(),
                session.refreshExpiresAt());
    }

    private AccountIdentityStorePort.IdentityRecord touch(
            AccountIdentityStorePort.IdentityRecord identity, String email) {
        return identities.saveIdentity(
                new AccountIdentityStorePort.IdentityRecord(
                        identity.id(),
                        identity.userId(),
                        identity.provider(),
                        identity.providerSubject(),
                        email,
                        identity.createdAt(),
                        clock.instant(),
                        identity.version()));
    }

    private UserAccount loadAccount(UUID userId) {
        return accounts.findById(new UserId(userId))
                .orElseThrow(
                        () -> EarthTripException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    }

    private static String provider(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!List.of("APPLE", "GOOGLE").contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_OAUTH_PROVIDER", "Apple과 Google 로그인만 지원합니다.");
        }
        return normalized;
    }

    private static IdentityResult result(AccountIdentityStorePort.IdentityRecord identity) {
        return new IdentityResult(
                identity.id(),
                identity.provider(),
                identity.providerEmail(),
                identity.createdAt(),
                identity.lastUsedAt(),
                identity.version());
    }
}
