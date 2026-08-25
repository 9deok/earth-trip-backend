package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "public_trip_comments")
class PublicTripCommentJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "publication_id", nullable = false, length = 36)
    private String publicationId;

    @Column(name = "actor_user_id", nullable = false, length = 36)
    private String actorUserId;

    @Column(name = "body", nullable = false, length = 800)
    private String body;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PublicTripCommentJpaEntity() {}

    PublicTripCommentJpaEntity(PublicTripEngagementStorePort.CommentRecord record) {
        id = record.id().toString();
        publicationId = record.publicationId().toString();
        actorUserId = record.actorUserId().toString();
        body = record.body();
        status = record.status();
        createdAt = record.createdAt();
        updatedAt = record.updatedAt();
    }

    PublicTripEngagementStorePort.CommentRecord toRecord() {
        return new PublicTripEngagementStorePort.CommentRecord(
                UUID.fromString(id),
                UUID.fromString(publicationId),
                UUID.fromString(actorUserId),
                body,
                status,
                createdAt,
                updatedAt);
    }
}
