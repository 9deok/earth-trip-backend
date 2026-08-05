package com.earthtrip.notification.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesPushTokenProtectorTest {

    @Test
    void encryptedDeviceTokenCanBeRevealedForDelivery() {
        String key = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );
        AesPushTokenProtector protector = new AesPushTokenProtector(key);

        var protectedToken = protector.protect("fcm-device-token");

        assertThat(protectedToken.cipher()).doesNotContain("fcm-device-token");
        assertThat(protector.reveal(protectedToken.cipher())).isEqualTo("fcm-device-token");
        assertThat(protectedToken.hash()).hasSize(64);
    }
}
