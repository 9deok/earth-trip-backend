package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.ActivityFeedUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ActivityFeedService implements ActivityFeedUseCase {

    private final TripAccess access;
    private final PlanningResourceUseCase resources;
    private final ActivityOperationStorePort activities;
    private final SyncStateStorePort syncState;
    private final Clock clock;

    ActivityFeedService(
            TripAccess access,
            PlanningResourceUseCase resources,
            ActivityOperationStorePort activities,
            SyncStateStorePort syncState,
            Clock clock) {
        this.access = access;
        this.resources = resources;
        this.activities = activities;
        this.syncState = syncState;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityPage activities(UUID tripId, UUID actorUserId, Long after, Integer limit) {
        access.requireViewer(tripId, actorUserId);
        int pageSize = pageSize(limit);
        List<ActivityOperationStorePort.ActivityRecord> rows =
                activities.activities(tripId, cursor(after), pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<ActivityItem> items = rows.stream().limit(pageSize).map(this::item).toList();
        long nextCursor = items.isEmpty() ? cursor(after) : items.getLast().sequenceId();
        return new ActivityPage(
                items, nextCursor, hasMore, syncState.readCursor(tripId, actorUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ChangePage changes(UUID tripId, UUID actorUserId, Long after, Integer limit) {
        access.requireViewer(tripId, actorUserId);
        int pageSize = pageSize(limit);
        List<ActivityOperationStorePort.ActivityRecord> rows =
                activities.activities(tripId, cursor(after), pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<ActivityItem> items = rows.stream().limit(pageSize).map(this::item).toList();
        long nextCursor = items.isEmpty() ? cursor(after) : items.getLast().sequenceId();
        return new ChangePage(items, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public ChangeCursorResult latestCursor(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return new ChangeCursorResult(activities.latestSequence(tripId));
    }

    @Override
    public ReadCursorResult updateReadCursor(UUID tripId, UUID actorUserId, long sequenceId) {
        access.requireViewer(tripId, actorUserId);
        long latest = activities.latestSequence(tripId);
        long current = syncState.readCursor(tripId, actorUserId);
        if (sequenceId < current || sequenceId > latest) {
            throw EarthTripException.badRequest(
                    "INVALID_ACTIVITY_CURSOR", "읽음 위치는 현재 위치보다 작거나 최신 변경보다 클 수 없습니다.");
        }
        SyncStateStorePort.ReadCursorRecord saved =
                syncState.saveReadCursor(
                        new SyncStateStorePort.ReadCursorRecord(
                                tripId, actorUserId, sequenceId, clock.instant()));
        return new ReadCursorResult(saved.sequenceId(), saved.updatedAt());
    }

    @Override
    public RevertResult revert(
            UUID tripId, UUID activityId, UUID actorUserId, long resourceBaseVersion) {
        access.requireEditor(tripId, actorUserId);
        ActivityOperationStorePort.ActivityRecord activity =
                activities
                        .findActivity(activityId)
                        .filter(item -> item.tripId().equals(tripId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "ACTIVITY_NOT_FOUND", "활동 이력을 찾을 수 없습니다."));
        if (!activity.action().equals("CREATED")) {
            throw EarthTripException.badRequest(
                    "ACTIVITY_NOT_REVERSIBLE", "이 활동은 이전 값 스냅샷이 없어 자동으로 되돌릴 수 없습니다.");
        }
        resources.delete(
                tripId,
                actorUserId,
                activity.resourceType(),
                activity.resourceId(),
                PlanningResourceUseCase.WritePermission.EDITOR,
                resourceBaseVersion);
        return new RevertResult(activityId, "REVERTED", activity.resourceId());
    }

    private ActivityItem item(ActivityOperationStorePort.ActivityRecord record) {
        return new ActivityItem(
                record.sequenceId(),
                record.activityId(),
                record.actorId(),
                record.action(),
                record.resourceType(),
                record.resourceId(),
                record.details(),
                record.occurredAt(),
                record.action().equals("CREATED"));
    }

    private static long cursor(Long value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw EarthTripException.badRequest("INVALID_CHANGE_CURSOR", "변경 커서는 0 이상이어야 합니다.");
        }
        return value;
    }

    private static int pageSize(Integer value) {
        if (value == null) {
            return 50;
        }
        if (value < 1 || value > 100) {
            throw EarthTripException.badRequest("INVALID_PAGE_SIZE", "페이지 크기는 1~100이어야 합니다.");
        }
        return value;
    }
}
