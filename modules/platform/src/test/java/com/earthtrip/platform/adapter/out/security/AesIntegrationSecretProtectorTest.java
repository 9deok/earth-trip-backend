package com.earthtrip.platform.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesIntegrationSecretProtectorTest {

    private static final String KEY =
            Base64.getEncoder()
                    .encodeToString(
                            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptsAndDecryptsRefreshTokenWithPurposeBinding() {
        AesIntegrationSecretProtector protector =
                new AesIntegrationSecretProtector("primary:" + KEY, "primary");

        String protectedValue = protector.protect("google-calendar-refresh-token", "refresh-123");

        assertThat(protectedValue).doesNotContain("refresh-123");
        assertThat(protector.reveal("google-calendar-refresh-token", protectedValue))
                .isEqualTo("refresh-123");
        assertThatThrownBy(() -> protector.reveal("another-purpose", protectedValue))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void keepsPreviousKeyAvailableDuringRotation() {
        AesIntegrationSecretProtector oldProtector =
                new AesIntegrationSecretProtector("old:" + KEY, "old");
        String protectedValue = oldProtector.protect("calendar", "refresh-token");
        String newKey =
                Base64.getEncoder()
                        .encodeToString(
                                "abcdefghijklmnopqrstuvwxyzABCDEF"
                                        .getBytes(StandardCharsets.UTF_8));
        AesIntegrationSecretProtector rotated =
                new AesIntegrationSecretProtector("new:" + newKey + ",old:" + KEY, "new");

        assertThat(rotated.reveal("calendar", protectedValue)).isEqualTo("refresh-token");
    }
}
