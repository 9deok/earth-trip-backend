package com.earthtrip.identity.application.service.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 삭제된_계정의_남은_refresh_token은_새_토큰으로_교환하지_않는다() {
        UserId userId = new UserId(UUID.randomUUID());
        AuthSession session =
                AuthSession.create(
                        UUID.randomUUID(),
                        userId,
                        "access-hash",
                        "refresh-hash",
                        "test-device",
                        NOW.plusSeconds(60),
                        NOW.plusSeconds(3_600),
                        NOW);
        UserAccount deleted =
                UserAccount.restore(
                        userId,
                        new EmailAddress("deleted@example.com"),
                        "password-hash",
                        "삭제 계정",
                        UserAccount.Status.DELETED,
                        NOW,
                        NOW,
                        NOW);
        AuthSessionStorePort sessions = mock(AuthSessionStorePort.class);
        UserAccountStorePort accounts = mock(UserAccountStorePort.class);
        CredentialPort credentials = mock(CredentialPort.class);
        when(credentials.hashToken("refresh-token")).thenReturn("refresh-hash");
        when(sessions.findByRefreshTokenHash("refresh-hash")).thenReturn(Optional.of(session));
        when(accounts.findById(userId)).thenReturn(Optional.of(deleted));
        SessionService service =
                new SessionService(
                        accounts, sessions, credentials, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.refresh("refresh-token"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("INVALID_REFRESH_TOKEN"));

        verify(credentials, never()).newToken();
        verify(sessions, never()).save(session);
    }
}
