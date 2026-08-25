package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PublicTripEngagementStorePort {

    boolean hasReaction(UUID publicationId, UUID actorUserId, String reactionType);

    void saveReaction(ReactionRecord reaction);

    void deleteReaction(UUID publicationId, UUID actorUserId, String reactionType);

    long countReactions(UUID publicationId, String reactionType);

    CommentRecord saveComment(CommentRecord comment);

    List<CommentRecord> findComments(UUID publicationId, int limit);

    long countComments(UUID publicationId);

    record ReactionRecord(
            UUID publicationId,
            UUID actorUserId,
            String reactionType,
            Instant createdAt,
            Instant updatedAt) {}

    record CommentRecord(
            UUID id,
            UUID publicationId,
            UUID actorUserId,
            String body,
            String status,
            Instant createdAt,
            Instant updatedAt) {}
}
