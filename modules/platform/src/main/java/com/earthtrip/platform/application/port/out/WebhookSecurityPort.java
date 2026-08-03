package com.earthtrip.platform.application.port.out;

public interface WebhookSecurityPort {

    VerifiedWebhook verify(
        String provider,
        String eventId,
        String timestamp,
        String signature,
        String rawBody
    );

    record VerifiedWebhook(
        String provider,
        String eventId,
        String payloadDigest
    ) { }
}
