package com.earthtrip.identity.adapter.out.persistence.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthSessionAuthenticationRowTest {

    @Test
    void 공개_스칼라_조회_결과에서_인증_세션을_복원한다() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-06T06:56:30Z");
        AuthSessionAuthenticationRow row =
                new AuthSessionAuthenticationRow(
                        sessionId.toString(),
                        userId.toString(),
                        "access-hash",
                        "refresh-hash",
                        "test-device",
                        now.plusSeconds(3_600),
                        now.plusSeconds(86_400),
                        now,
                        null,
                        now,
                        "여행자",
                        "ACTIVE");

        var session = row.toDomain();

        assertThat(Modifier.isPublic(AuthSessionAuthenticationRow.class.getModifiers())).isTrue();
        assertThat(session.id()).isEqualTo(sessionId);
        assertThat(session.userId().value()).isEqualTo(userId);
        assertThat(session.acceptsAccessAt(now)).isTrue();
        assertThat(row.displayName()).isEqualTo("여행자");
        assertThat(row.accountStatus()).isEqualTo("ACTIVE");
        assertThat(row.accountCanSignIn()).isTrue();
        assertThat(
                        new AuthSessionAuthenticationRow(
                                        sessionId.toString(),
                                        userId.toString(),
                                        "access-hash",
                                        "refresh-hash",
                                        "test-device",
                                        now.plusSeconds(3_600),
                                        now.plusSeconds(86_400),
                                        now,
                                        null,
                                        now,
                                        "여행자",
                                        "DELETION_PENDING")
                                .accountCanSignIn())
                .isTrue();
    }
}
