package com.earthtrip.identity.adapter.out.persistence.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ownership_transfers")
class OwnershipTransferJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "from_user_id", nullable = false, length = 36)
    private String fromUserId;

    @Column(name = "to_user_id", nullable = false, length = 36)
    private String toUserId;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OwnershipTransferJpaEntity() {}

    OwnershipTransferJpaEntity(
            UUID id, UUID tripId, UUID fromUserId, UUID toUserId, Instant confirmedAt) {
        this.id = id.toString();
        this.tripId = tripId.toString();
        this.fromUserId = fromUserId.toString();
        this.toUserId = toUserId.toString();
        this.confirmedAt = confirmedAt;
        this.createdAt = confirmedAt;
    }
}
