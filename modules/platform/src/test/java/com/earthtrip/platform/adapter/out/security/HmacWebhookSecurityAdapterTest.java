package com.earthtrip.platform.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class HmacWebhookSecurityAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private static final String SECRET = "test-secret-at-least-random";

    @Test
    void 유효한_HMAC과_timestamp를_검증한다() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("earthtrip.internal.webhooks.malware-scan.secret", SECRET)
            .withProperty("earthtrip.internal.webhook-max-age-seconds", "300");
        HmacWebhookSecurityAdapter adapter = new HmacWebhookSecurityAdapter(
            environment,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        String eventId = "scan-event-1";
        String timestamp = Long.toString(NOW.getEpochSecond());
        String body = "{\"fileId\":\"5c978827-2701-44b2-a835-683651f293ef\"}";

        var verified = adapter.verify(
            "malware-scan",
            eventId,
            timestamp,
            "sha256=" + signature(timestamp, eventId, body),
            body
        );

        assertThat(verified.provider()).isEqualTo("malware-scan");
        assertThat(verified.eventId()).isEqualTo(eventId);
        assertThat(verified.payloadDigest()).hasSize(64);
    }

    @Test
    void 만료_timestamp와_잘못된_서명을_거부한다() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("earthtrip.internal.webhooks.calendar.secret", SECRET)
            .withProperty("earthtrip.internal.webhook-max-age-seconds", "300");
        HmacWebhookSecurityAdapter adapter = new HmacWebhookSecurityAdapter(
            environment,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> adapter.verify(
            "calendar",
            "event-1",
            Long.toString(NOW.minusSeconds(301).getEpochSecond()),
            "sha256=" + "00".repeat(32),
            "{}"
        )).isInstanceOfSatisfying(EarthTripException.class, exception ->
            assertThat(exception.code()).isEqualTo("WEBHOOK_TIMESTAMP_EXPIRED")
        );

        String timestamp = Long.toString(NOW.getEpochSecond());
        assertThatThrownBy(() -> adapter.verify(
            "calendar",
            "event-1",
            timestamp,
            "sha256=" + "00".repeat(32),
            "{}"
        )).isInstanceOfSatisfying(EarthTripException.class, exception ->
            assertThat(exception.code()).isEqualTo("INVALID_WEBHOOK_SIGNATURE")
        );
    }

    private static String signature(String timestamp, String eventId, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + eventId + "." + body).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (java.security.GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }
}
