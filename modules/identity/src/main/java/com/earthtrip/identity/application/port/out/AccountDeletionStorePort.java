package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionStorePort {

    DeletionRecord createOrGet(UserId userId, Instant requestedAt, Instant scheduledAt);

    Optional<DeletionRecord> findPending(UserId userId);

    void cancel(UserId userId, Instant cancelledAt);

    record DeletionRecord(UUID id, Instant requestedAt, Instant scheduledAt, String status) {}
}
