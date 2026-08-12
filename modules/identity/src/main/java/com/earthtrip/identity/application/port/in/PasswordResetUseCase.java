package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface PasswordResetUseCase {

    RequestResult request(String email);

    void reset(String token, String newPassword);

    record RequestResult(UUID requestId, Instant expiresAt, String deliveryStatus) {}
}
