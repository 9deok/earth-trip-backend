package com.earthtrip.wallet.application.port.out;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReservationChangeStorePort {

    Optional<ChangeSetRecord> find(UUID changeSetId);

    ChangeSetRecord save(ChangeSetRecord record);

    record ChangeSetRecord(
        UUID id,
        UUID tripId,
        UUID reservationId,
        UUID requestedBy,
        String proposalHash,
        Map<String, Object> beforeSnapshot,
        Map<String, Object> afterSnapshot,
        Instant appliedAt
    ) { }
}
