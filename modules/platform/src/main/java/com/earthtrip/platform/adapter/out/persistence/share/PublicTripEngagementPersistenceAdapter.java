package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class PublicTripEngagementPersistenceAdapter implements PublicTripEngagementStorePort {
    private final PublicTripReactionJpaRepository reactions;
    private final PublicTripCommentJpaRepository comments;

    PublicTripEngagementPersistenceAdapter(
            PublicTripReactionJpaRepository reactions, PublicTripCommentJpaRepository comments) {
        this.reactions = reactions;
        this.comments = comments;
    }

    @Override
    public boolean hasReaction(UUID publicationId, UUID actorUserId, String reactionType) {
        return reactions.existsById(id(publicationId, actorUserId, reactionType));
    }

    @Override
    public void saveReaction(ReactionRecord reaction) {
        reactions.save(new PublicTripReactionJpaEntity(reaction));
    }

    @Override
    public void deleteReaction(UUID publicationId, UUID actorUserId, String reactionType) {
        reactions.deleteById(id(publicationId, actorUserId, reactionType));
    }

    @Override
    public long countReactions(UUID publicationId, String reactionType) {
        return reactions.countByIdPublicationIdAndIdReactionType(
                publicationId.toString(), reactionType);
    }

    @Override
    public CommentRecord saveComment(CommentRecord comment) {
        return comments.save(new PublicTripCommentJpaEntity(comment)).toRecord();
    }

    @Override
    public List<CommentRecord> findComments(UUID publicationId, int limit) {
        return comments
                .findByPublicationIdAndStatusOrderByCreatedAtDesc(
                        publicationId.toString(),
                        "ACTIVE",
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(PublicTripCommentJpaEntity::toRecord)
                .toList();
    }

    @Override
    public long countComments(UUID publicationId) {
        return comments.countByPublicationIdAndStatus(publicationId.toString(), "ACTIVE");
    }

    private static PublicTripReactionJpaId id(
            UUID publicationId, UUID actorUserId, String reactionType) {
        return new PublicTripReactionJpaId(
                publicationId.toString(), actorUserId.toString(), reactionType);
    }
}
