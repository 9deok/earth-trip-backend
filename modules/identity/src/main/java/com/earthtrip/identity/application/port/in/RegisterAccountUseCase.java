package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface RegisterAccountUseCase {

    Result register(Command command);

    record Command(UUID requestId, String email, String password, String displayName) {}

    record Result(
            UUID userId, String email, String displayName, String status, Instant createdAt) {}
}
