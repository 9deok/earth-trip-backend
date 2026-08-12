package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ActivityFeedUseCase {

    ActivityPage activities(UUID tripId, UUID actorUserId, Long after, Integer limit);

    ChangePage changes(UUID tripId, UUID actorUserId, Long after, Integer limit);

    ChangeCursorResult latestCursor(UUID tripId, UUID actorUserId);

    ReadCursorResult updateReadCursor(UUID tripId, UUID actorUserId, long sequenceId);

    RevertResult revert(UUID tripId, UUID activityId, UUID actorUserId, long resourceBaseVersion);

    record ActivityItem(
            long sequenceId,
            UUID activityId,
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> details,
            Instant occurredAt,
            boolean revertible) {}

    record ActivityPage(
            List<ActivityItem> items, long nextCursor, boolean hasMore, long readCursor) {}

    record ChangePage(List<ActivityItem> changes, long nextCursor, boolean hasMore) {}

    record ChangeCursorResult(long cursor) {}

    record ReadCursorResult(long sequenceId, Instant updatedAt) {}

    record RevertResult(UUID activityId, String status, UUID resourceId) {}
}
