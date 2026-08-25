package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "public_trip_reactions")
class PublicTripReactionJpaEntity {
    @EmbeddedId private PublicTripReactionJpaId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PublicTripReactionJpaEntity() {}

    PublicTripReactionJpaEntity(PublicTripEngagementStorePort.ReactionRecord record) {
        id =
                new PublicTripReactionJpaId(
                        record.publicationId().toString(),
                        record.actorUserId().toString(),
                        record.reactionType());
        createdAt = record.createdAt();
        updatedAt = record.updatedAt();
    }

    PublicTripEngagementStorePort.ReactionRecord toRecord() {
        return new PublicTripEngagementStorePort.ReactionRecord(
                UUID.fromString(id.publicationId()),
                UUID.fromString(id.actorUserId()),
                id.reactionType(),
                createdAt,
                updatedAt);
    }
}
