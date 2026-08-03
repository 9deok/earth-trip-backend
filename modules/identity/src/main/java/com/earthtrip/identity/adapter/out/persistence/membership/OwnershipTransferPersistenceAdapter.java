package com.earthtrip.identity.adapter.out.persistence.membership;

import com.earthtrip.identity.application.port.out.OwnershipTransferStorePort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OwnershipTransferPersistenceAdapter implements OwnershipTransferStorePort {
    private final OwnershipTransferJpaRepository repository;
    OwnershipTransferPersistenceAdapter(OwnershipTransferJpaRepository repository) {
        this.repository = repository;
    }
    @Override
    public void record(UUID id, UUID tripId, UUID fromUserId, UUID toUserId, Instant confirmedAt) {
        repository.save(new OwnershipTransferJpaEntity(
            id, tripId, fromUserId, toUserId, confirmedAt
        ));
    }
}
