package com.earthtrip.platform.adapter.out.security;

import com.earthtrip.platform.application.port.out.WebhookSecurityPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class HmacWebhookSecurityAdapter implements WebhookSecurityPort {

    private static final Set<String> PROVIDERS =
            Set.of(
                    "object-storage",
                    "malware-scan",
                    "calendar",
                    "push-delivery",
                    "financial-provider");

    private final Environment environment;
    private final Clock clock;

    HmacWebhookSecurityAdapter(Environment environment, Clock clock) {
        this.environment = environment;
        this.clock = clock;
    }

    @Override
    public VerifiedWebhook verify(
            String provider, String eventId, String timestamp, String signature, String rawBody) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedEventId = requireHeader(eventId, "WEBHOOK_EVENT_ID_REQUIRED");
        if (normalizedEventId.length() > 160) {
            throw EarthTripException.badRequest("WEBHOOK_EVENT_ID_TOO_LONG", "웹훅 이벤트 ID가 너무 깁니다.");
        }
        String normalizedTimestamp = requireHeader(timestamp, "WEBHOOK_TIMESTAMP_REQUIRED");
        Instant occurredAt = parseTimestamp(normalizedTimestamp);
        long maxAgeSeconds =
                environment.getProperty(
                        "earthtrip.internal.webhook-max-age-seconds", Long.class, 300L);
        if (Duration.between(occurredAt, clock.instant()).abs().getSeconds() > maxAgeSeconds) {
            throw EarthTripException.unauthorized(
                    "WEBHOOK_TIMESTAMP_EXPIRED", "웹훅 timestamp가 허용 시간 범위를 벗어났습니다.");
        }
        String secret =
                environment.getProperty(
                        "earthtrip.internal.webhooks." + normalizedProvider + ".secret", "");
        if (secret.isBlank()) {
            throw EarthTripException.unavailable(
                    "WEBHOOK_SECRET_NOT_CONFIGURED", normalizedProvider + " 웹훅 서명 키가 설정되지 않았습니다.");
        }
        String normalizedBody = rawBody == null ? "" : rawBody;
        byte[] expected =
                hmac(secret, normalizedTimestamp + "." + normalizedEventId + "." + normalizedBody);
        byte[] supplied = decodeSignature(signature);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw EarthTripException.unauthorized("INVALID_WEBHOOK_SIGNATURE", "웹훅 서명이 올바르지 않습니다.");
        }
        return new VerifiedWebhook(
                normalizedProvider,
                normalizedEventId,
                HexFormat.of().formatHex(sha256(normalizedBody)));
    }

    private static String normalizeProvider(String provider) {
        String normalized =
                requireHeader(provider, "WEBHOOK_PROVIDER_REQUIRED").toLowerCase(Locale.ROOT);
        if (!PROVIDERS.contains(normalized)) {
            throw EarthTripException.notFound("WEBHOOK_PROVIDER_NOT_FOUND", "지원하지 않는 웹훅 제공자입니다.");
        }
        return normalized;
    }

    private static Instant parseTimestamp(String value) {
        try {
            if (value.chars().allMatch(Character::isDigit)) {
                return Instant.ofEpochSecond(Long.parseLong(value));
            }
            return Instant.parse(value);
        } catch (DateTimeParseException | ArithmeticException | NumberFormatException exception) {
            throw EarthTripException.badRequest(
                    "INVALID_WEBHOOK_TIMESTAMP",
                    "웹훅 timestamp는 epoch seconds 또는 ISO-8601 형식이어야 합니다.");
        }
    }

    private static byte[] decodeSignature(String value) {
        String signature = requireHeader(value, "WEBHOOK_SIGNATURE_REQUIRED");
        if (signature.regionMatches(true, 0, "sha256=", 0, 7)) {
            signature = signature.substring(7);
        }
        try {
            return HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.unauthorized(
                    "INVALID_WEBHOOK_SIGNATURE", "웹훅 서명 형식이 올바르지 않습니다.");
        }
    }

    private static byte[] hmac(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256을 초기화할 수 없습니다.", exception);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 초기화할 수 없습니다.", exception);
        }
    }

    private static String requireHeader(String value, String code) {
        if (value == null || value.isBlank()) {
            throw EarthTripException.badRequest(code, "필수 웹훅 헤더가 누락되었습니다.");
        }
        return value.strip();
    }
}
