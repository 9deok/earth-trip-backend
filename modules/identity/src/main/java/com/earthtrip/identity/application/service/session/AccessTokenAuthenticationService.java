package com.earthtrip.identity.application.service.session;

import com.earthtrip.identity.application.port.in.AccessTokenAuthenticationUseCase;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AccessTokenAuthenticationService implements AccessTokenAuthenticationUseCase {

    private final AuthSessionStorePort sessionStore;
    private final UserAccountStorePort accountStore;
    private final CredentialPort credentialPort;
    private final Clock clock;

    AccessTokenAuthenticationService(
            AuthSessionStorePort sessionStore,
            UserAccountStorePort accountStore,
            CredentialPort credentialPort,
            Clock clock) {
        this.sessionStore = sessionStore;
        this.accountStore = accountStore;
        this.credentialPort = credentialPort;
        this.clock = clock;
    }

    @Override
    public AuthenticationResult authenticate(String rawAccessToken) {
        String tokenHash = credentialPort.hashToken(rawAccessToken);
        AuthSessionStorePort.AuthenticatedSessionRecord authentication =
                sessionStore.findAuthenticationByAccessTokenHash(tokenHash).orElse(null);
        if (authentication != null) {
            AuthSession session = authentication.session();
            if (!session.acceptsAccessAt(clock.instant()) || !authentication.accountCanSignIn()) {
                throw invalidToken();
            }
            return new AuthenticationResult(
                    session.userId().value(), session.id(), authentication.displayName());
        }
        AuthSession session =
                sessionStore
                        .findByAccessTokenHash(tokenHash)
                        .orElseThrow(AccessTokenAuthenticationService::invalidToken);
        if (!session.acceptsAccessAt(clock.instant())) throw invalidToken();
        UserAccount account =
                accountStore
                        .findById(session.userId())
                        .filter(UserAccount::canSignIn)
                        .orElseThrow(AccessTokenAuthenticationService::invalidToken);
        return new AuthenticationResult(account.id().value(), session.id(), account.displayName());
    }

    private static EarthTripException invalidToken() {
        return EarthTripException.unauthorized("INVALID_ACCESS_TOKEN", "로그인 세션이 만료되었거나 올바르지 않습니다.");
    }
}
