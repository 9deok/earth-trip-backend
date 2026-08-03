package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface OwnershipTransferStorePort {
    void record(UUID id, UUID tripId, UUID fromUserId, UUID toUserId, Instant confirmedAt);
}
