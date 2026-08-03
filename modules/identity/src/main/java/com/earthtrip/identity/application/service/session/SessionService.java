package com.earthtrip.identity.application.service.session;

import com.earthtrip.identity.application.port.in.SessionUseCase;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.earthtrip.identity.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SessionService implements SessionUseCase {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    private final UserAccountStorePort accountStore;
    private final AuthSessionStorePort sessionStore;
    private final CredentialPort credentialPort;
    private final Clock clock;

    SessionService(
        UserAccountStorePort accountStore,
        AuthSessionStorePort sessionStore,
        CredentialPort credentialPort,
        Clock clock
    ) {
        this.accountStore = accountStore;
        this.sessionStore = sessionStore;
        this.credentialPort = credentialPort;
        this.clock = clock;
    }

    @Override
    public SessionResult create(String email, String password, String deviceName) {
        UserAccount account = accountStore.findByEmail(new EmailAddress(email))
            .orElseThrow(SessionService::invalidCredentials);
        if (!credentialPort.passwordMatches(password, account.passwordHash())) {
            throw invalidCredentials();
        }
        if (!account.canSignIn()) {
            String code = account.status() == UserAccount.Status.PENDING_VERIFICATION
                ? "EMAIL_VERIFICATION_REQUIRED"
                : "ACCOUNT_UNAVAILABLE";
            throw EarthTripException.forbidden(code, "현재 이 계정으로 로그인할 수 없습니다.");
        }
        Instant now = clock.instant();
        String accessToken = credentialPort.newToken();
        String refreshToken = credentialPort.newToken();
        AuthSession session = AuthSession.create(
            UUID.randomUUID(),
            account.id(),
            credentialPort.hashToken(accessToken),
            credentialPort.hashToken(refreshToken),
            deviceName,
            now.plus(ACCESS_TTL),
            now.plus(REFRESH_TTL),
            now
        );
        sessionStore.save(session);
        return result(session, accessToken, refreshToken);
    }

    @Override
    public SessionResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw EarthTripException.unauthorized("INVALID_REFRESH_TOKEN", "갱신 토큰이 필요합니다.");
        }
        AuthSession session = sessionStore.findByRefreshTokenHash(
            credentialPort.hashToken(rawRefreshToken)
        ).orElseThrow(() -> EarthTripException.unauthorized(
            "INVALID_REFRESH_TOKEN",
            "올바르지 않은 갱신 토큰입니다."
        ));
        Instant now = clock.instant();
        if (!session.acceptsRefreshAt(now)) {
            throw EarthTripException.unauthorized("REFRESH_TOKEN_EXPIRED", "세션을 다시 시작해 주세요.");
        }
        String accessToken = credentialPort.newToken();
        String refreshToken = credentialPort.newToken();
        session.rotate(
            credentialPort.hashToken(accessToken),
            credentialPort.hashToken(refreshToken),
            now.plus(ACCESS_TTL),
            now.plus(REFRESH_TTL),
            now
        );
        sessionStore.save(session);
        return result(session, accessToken, refreshToken);
    }

    @Override
    public void revoke(UUID sessionId, UUID actorUserId) {
        AuthSession session = sessionStore.findById(sessionId)
            .orElseThrow(() -> EarthTripException.notFound("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."));
        if (!session.userId().value().equals(actorUserId)) {
            throw EarthTripException.notFound("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.");
        }
        session.revoke(clock.instant());
        sessionStore.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSessionResult> list(UUID userId, UUID currentSessionId) {
        Instant now = clock.instant();
        return sessionStore.findByUserId(new UserId(userId)).stream()
            .map(session -> new DeviceSessionResult(
                session.id(),
                session.deviceName(),
                session.id().equals(currentSessionId),
                session.acceptsRefreshAt(now),
                session.lastUsedAt(),
                session.createdAt()
            ))
            .toList();
    }

    @Override
    public void revokeOtherSessions(UUID userId, UUID currentSessionId, boolean includeCurrent) {
        Instant now = clock.instant();
        sessionStore.findByUserId(new UserId(userId)).stream()
            .filter(session -> includeCurrent || !session.id().equals(currentSessionId))
            .forEach(session -> {
                session.revoke(now);
                sessionStore.save(session);
            });
    }

    private static SessionResult result(
        AuthSession session,
        String accessToken,
        String refreshToken
    ) {
        return new SessionResult(
            session.id(),
            session.userId().value(),
            accessToken,
            refreshToken,
            session.accessExpiresAt(),
            session.refreshExpiresAt()
        );
    }

    private static EarthTripException invalidCredentials() {
        return EarthTripException.unauthorized(
            "INVALID_CREDENTIALS",
            "이메일 또는 비밀번호가 올바르지 않습니다."
        );
    }
}
