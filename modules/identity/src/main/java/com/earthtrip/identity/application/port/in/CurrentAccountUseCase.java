package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface CurrentAccountUseCase {

    AccountResult get(UUID userId);

    AccountResult updateName(UUID userId, String displayName);

    DeletionResult requestDeletion(UUID userId);

    DeletionResult currentDeletion(UUID userId);

    void cancelDeletion(UUID userId);

    record AccountResult(
        UUID userId,
        String email,
        String displayName,
        String status,
        Instant emailVerifiedAt,
        Instant createdAt,
        Instant updatedAt
    ) { }

    record DeletionResult(
        UUID requestId,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        String status
    ) { }
}
