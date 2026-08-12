package com.earthtrip.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 삭제_유예_기간에는_다시_로그인해_삭제를_취소할_수_있다() {
        UserAccount account =
                UserAccount.register(
                        new UserId(UUID.randomUUID()),
                        new EmailAddress("traveler@example.com"),
                        "password-hash",
                        "여행자",
                        NOW);
        account.verifyEmail(NOW);
        account.markDeletionPending(NOW.plusSeconds(1));

        assertThat(account.canSignIn()).isTrue();
    }
}
