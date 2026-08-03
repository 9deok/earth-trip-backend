package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface SupportRequestUseCase {

    SupportResult create(
        UUID actorUserId,
        UUID requestId,
        String category,
        String description,
        String traceId,
        Map<String, Object> diagnostics,
        boolean diagnosticsConsent
    );

    record SupportResult(UUID supportRequestId, String status, Instant createdAt) { }
}
