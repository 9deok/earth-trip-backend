package com.earthtrip.platform.application.service.share;

import com.earthtrip.platform.application.port.in.PublicTripEngagementUseCase;
import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class PublicTripEngagementService implements PublicTripEngagementUseCase {
    private static final Set<String> REACTION_TYPES = Set.of("LIKE", "HELPFUL");

    private final TripShareStorePort shares;
    private final PublicTripEngagementStorePort engagement;
    private final ShareAccessRecorder accessRecorder;
    private final Clock clock;

    PublicTripEngagementService(
            TripShareStorePort shares,
            PublicTripEngagementStorePort engagement,
            ShareAccessRecorder accessRecorder,
            Clock clock) {
        this.shares = shares;
        this.engagement = engagement;
        this.accessRecorder = accessRecorder;
        this.clock = clock;
    }

    @Override
    public EngagementResult engagement(UUID publicationId, UUID actorUserId) {
        requirePublic(publicationId);
        return result(publicationId, actorUserId);
    }

    @Override
    @Transactional
    public EngagementResult setReaction(
            UUID publicationId, UUID actorUserId, String reactionType, boolean active) {
        requirePublic(publicationId);
        String type = reactionType == null ? "" : reactionType.strip().toUpperCase();
        if (!REACTION_TYPES.contains(type)) {
            throw EarthTripException.badRequest(
                    "INVALID_PUBLIC_TRIP_REACTION", "지원하지 않는 공개 여행 반응입니다.");
        }
        boolean exists = engagement.hasReaction(publicationId, actorUserId, type);
        if (active && !exists) {
            Instant now = clock.instant();
            engagement.saveReaction(
                    new PublicTripEngagementStorePort.ReactionRecord(
                            publicationId, actorUserId, type, now, now));
        } else if (!active && exists) {
            engagement.deleteReaction(publicationId, actorUserId, type);
        }
        return result(publicationId, actorUserId);
    }

    @Override
    public List<CommentResult> comments(UUID publicationId, UUID actorUserId, int limit) {
        requirePublic(publicationId);
        return engagement.findComments(publicationId, limit).stream()
                .map(comment -> comment(comment, actorUserId))
                .toList();
    }

    @Override
    @Transactional
    public CommentResult addComment(UUID publicationId, UUID actorUserId, String body) {
        requirePublic(publicationId);
        String normalized = body == null ? "" : body.strip();
        if (normalized.isEmpty() || normalized.length() > 800) {
            throw EarthTripException.badRequest(
                    "INVALID_PUBLIC_TRIP_COMMENT", "댓글은 1자 이상 800자 이하로 입력해 주세요.");
        }
        Instant now = clock.instant();
        PublicTripEngagementStorePort.CommentRecord saved =
                engagement.saveComment(
                        new PublicTripEngagementStorePort.CommentRecord(
                                UUID.randomUUID(),
                                publicationId,
                                actorUserId,
                                normalized,
                                "ACTIVE",
                                now,
                                now));
        return comment(saved, actorUserId);
    }

    @Override
    @Transactional
    public EngagementResult recordCopy(UUID publicationId, UUID actorUserId) {
        requirePublic(publicationId);
        accessRecorder.record(publicationId, true, "COPIED_PUBLIC");
        return result(publicationId, actorUserId);
    }

    private EngagementResult result(UUID publicationId, UUID actorUserId) {
        return new EngagementResult(
                engagement.countReactions(publicationId, "LIKE"),
                engagement.countReactions(publicationId, "HELPFUL"),
                engagement.countComments(publicationId),
                shares.accessEvents(publicationId).stream()
                        .filter(TripShareStorePort.AccessRecord::success)
                        .filter(event -> event.reason().equals("COPIED_PUBLIC"))
                        .count(),
                actorUserId != null && engagement.hasReaction(publicationId, actorUserId, "LIKE"),
                actorUserId != null
                        && engagement.hasReaction(publicationId, actorUserId, "HELPFUL"));
    }

    private TripShareStorePort.ShareRecord requirePublic(UUID publicationId) {
        TripShareStorePort.ShareRecord share =
                shares.findById(publicationId).orElseThrow(PublicTripEngagementService::notFound);
        if (!share.status().equals("ACTIVE")
                || !share.visibility().equals("PUBLIC")
                || share.passwordHash() != null
                || share.expiresAt() != null && !share.expiresAt().isAfter(clock.instant())) {
            throw notFound();
        }
        return share;
    }

    private static CommentResult comment(
            PublicTripEngagementStorePort.CommentRecord comment, UUID actorUserId) {
        boolean mine = actorUserId != null && actorUserId.equals(comment.actorUserId());
        return new CommentResult(
                comment.id(), mine ? "나" : "여행자", comment.body(), mine, comment.createdAt());
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("PUBLIC_TRIP_NOT_FOUND", "공개된 여행 계획을 찾을 수 없습니다.");
    }
}
