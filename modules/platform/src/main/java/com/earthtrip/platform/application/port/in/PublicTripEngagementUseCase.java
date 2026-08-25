package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PublicTripEngagementUseCase {

    EngagementResult engagement(UUID publicationId, UUID actorUserId);

    EngagementResult setReaction(
            UUID publicationId, UUID actorUserId, String reactionType, boolean active);

    List<CommentResult> comments(UUID publicationId, UUID actorUserId, int limit);

    CommentResult addComment(UUID publicationId, UUID actorUserId, String body);

    EngagementResult recordCopy(UUID publicationId, UUID actorUserId);

    record EngagementResult(
            long likeCount,
            long helpfulCount,
            long commentCount,
            long copyCount,
            boolean likedByMe,
            boolean helpfulByMe) {}

    record CommentResult(
            UUID id, String authorName, String body, boolean mine, Instant createdAt) {}
}
