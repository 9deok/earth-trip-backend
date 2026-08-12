package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface EmailVerificationUseCase {

    RequestResult request(String email);

    ConfirmResult confirm(String token);

    record RequestResult(UUID requestId, Instant expiresAt, String deliveryStatus) {}

    record ConfirmResult(UUID userId, String email, Instant verifiedAt) {}
}
