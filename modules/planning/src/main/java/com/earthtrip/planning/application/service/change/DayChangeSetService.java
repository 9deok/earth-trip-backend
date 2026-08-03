package com.earthtrip.planning.application.service.change;

import com.earthtrip.planning.application.port.in.DayChangeSetUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DayChangeSetService implements DayChangeSetUseCase {

    private static final TypeReference<List<SnapshotItem>> SNAPSHOT_TYPE =
        new TypeReference<>() { };

    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;
    private final ScheduleChangeStorePort store;
    private final ObjectMapper json;
    private final Clock clock;

    DayChangeSetService(
        TripDayUseCase days,
        ScheduleUseCase schedule,
        ScheduleChangeStorePort store,
        ObjectMapper json,
        Clock clock
    ) {
        this.days = days;
        this.schedule = schedule;
        this.store = store;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public ChangeSetResult apply(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        ChangeSetCommand command
    ) {
        days.requireDay(tripId, dayId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest(
                "REQUEST_ID_REQUIRED", "requestId가 필요합니다."
            );
        }
        ScheduleChangeStorePort.ChangeSetRecord existing = store
            .findChangeSet(command.requestId())
            .orElse(null);
        if (existing != null) {
            requireSameScope(existing, tripId, dayId);
            return result(existing);
        }
        List<PlanningResourceUseCase.ResourceResult> current = schedule.list(
            tripId, dayId, actorUserId
        );
        validateOrder(current, command.order());
        List<SnapshotItem> before = snapshot(current);
        List<PlanningResourceUseCase.ResourceResult> saved = schedule.reorder(
            tripId, dayId, actorUserId,
            command.order().stream()
                .map(item -> new ScheduleUseCase.OrderItem(
                    item.itemId(), item.sortOrder(), item.baseVersion()
                ))
                .toList()
        );
        Instant now = clock.instant();
        return result(store.saveChangeSet(new ScheduleChangeStorePort.ChangeSetRecord(
            command.requestId(), tripId, dayId, actorUserId,
            write(before), write(snapshot(saved)), "APPLIED", now, null, 0
        )));
    }

    @Override
    public ChangeSetResult revert(
        UUID tripId,
        UUID dayId,
        UUID changeSetId,
        UUID actorUserId,
        long baseVersion
    ) {
        days.requireDay(tripId, dayId, actorUserId);
        ScheduleChangeStorePort.ChangeSetRecord changeSet = store
            .findChangeSet(changeSetId)
            .orElseThrow(() -> EarthTripException.notFound(
                "SCHEDULE_CHANGESET_NOT_FOUND", "일정 변경 이력을 찾을 수 없습니다."
            ));
        requireSameScope(changeSet, tripId, dayId);
        if (changeSet.status().equals("REVERTED")) {
            return result(changeSet);
        }
        if (changeSet.version() != baseVersion) {
            throw versionConflict(changeSet.version());
        }
        List<SnapshotItem> before = read(changeSet.beforeSnapshot());
        List<SnapshotItem> after = read(changeSet.afterSnapshot());
        List<PlanningResourceUseCase.ResourceResult> current = schedule.list(
            tripId, dayId, actorUserId
        );
        requireUnchangedAfterApply(current, after);
        Map<UUID, PlanningResourceUseCase.ResourceResult> currentById = current.stream()
            .collect(java.util.stream.Collectors.toMap(
                PlanningResourceUseCase.ResourceResult::resourceId,
                item -> item
            ));
        schedule.reorder(
            tripId, dayId, actorUserId,
            before.stream()
                .map(item -> new ScheduleUseCase.OrderItem(
                    item.itemId(), item.sortOrder(), currentById.get(item.itemId()).version()
                ))
                .toList()
        );
        return result(store.saveChangeSet(new ScheduleChangeStorePort.ChangeSetRecord(
            changeSet.id(), tripId, dayId, changeSet.requestedBy(),
            changeSet.beforeSnapshot(), changeSet.afterSnapshot(), "REVERTED",
            changeSet.appliedAt(), clock.instant(), changeSet.version()
        )));
    }

    private static void validateOrder(
        List<PlanningResourceUseCase.ResourceResult> current,
        List<OrderItem> order
    ) {
        if (order == null || order.size() != current.size()
            || order.stream().map(OrderItem::itemId).distinct().count() != order.size()
            || order.stream().map(OrderItem::sortOrder).distinct().count() != order.size()) {
            throw EarthTripException.badRequest(
                "INVALID_SCHEDULE_ORDER", "현재 날짜의 모든 일정을 중복 없이 포함해야 합니다."
            );
        }
        Map<UUID, Long> currentVersions = current.stream()
            .collect(java.util.stream.Collectors.toMap(
                PlanningResourceUseCase.ResourceResult::resourceId,
                PlanningResourceUseCase.ResourceResult::version
            ));
        for (OrderItem item : order) {
            Long version = currentVersions.get(item.itemId());
            if (version == null) {
                throw EarthTripException.badRequest(
                    "SCHEDULE_ITEM_NOT_IN_DAY", "다른 날짜의 일정이 포함되어 있습니다."
                );
            }
            if (version != item.baseVersion()) {
                throw versionConflict(version);
            }
        }
    }

    private static void requireUnchangedAfterApply(
        List<PlanningResourceUseCase.ResourceResult> current,
        List<SnapshotItem> after
    ) {
        Map<UUID, PlanningResourceUseCase.ResourceResult> currentById = current.stream()
            .collect(java.util.stream.Collectors.toMap(
                PlanningResourceUseCase.ResourceResult::resourceId,
                item -> item
            ));
        if (currentById.size() != after.size()) {
            throw cannotRevert();
        }
        for (SnapshotItem expected : after) {
            PlanningResourceUseCase.ResourceResult actual = currentById.get(expected.itemId());
            if (actual == null || actual.version() != expected.version()
                || actual.sortOrder() != expected.sortOrder()) {
                throw cannotRevert();
            }
        }
    }

    private static List<SnapshotItem> snapshot(
        List<PlanningResourceUseCase.ResourceResult> resources
    ) {
        return resources.stream()
            .map(item -> new SnapshotItem(
                item.resourceId(), item.sortOrder(), item.version()
            ))
            .sorted(Comparator.comparingInt(SnapshotItem::sortOrder))
            .toList();
    }

    private ChangeSetResult result(ScheduleChangeStorePort.ChangeSetRecord record) {
        List<SnapshotItem> before = read(record.beforeSnapshot());
        List<SnapshotItem> after = read(record.afterSnapshot());
        return new ChangeSetResult(
            record.id(), record.tripId(), record.dayId(), record.status(),
            ids(before), ids(after), record.appliedAt(), record.revertedAt(), record.version()
        );
    }

    private static List<UUID> ids(List<SnapshotItem> items) {
        return items.stream()
            .sorted(Comparator.comparingInt(SnapshotItem::sortOrder))
            .map(SnapshotItem::itemId)
            .toList();
    }

    private String write(List<SnapshotItem> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("일정 변경 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    private List<SnapshotItem> read(String value) {
        try {
            return json.readValue(value, SNAPSHOT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 일정 변경 스냅샷을 읽을 수 없습니다.", exception);
        }
    }

    private static void requireSameScope(
        ScheduleChangeStorePort.ChangeSetRecord record,
        UUID tripId,
        UUID dayId
    ) {
        if (!record.tripId().equals(tripId) || !record.dayId().equals(dayId)) {
            throw EarthTripException.conflict(
                "IDEMPOTENCY_KEY_REUSED", "이미 다른 일정 변경에 사용된 요청 ID입니다."
            );
        }
    }

    private static EarthTripException cannotRevert() {
        return EarthTripException.conflict(
            "SCHEDULE_CHANGED_AFTER_CHANGESET",
            "변경 적용 후 일정이 다시 수정되어 자동으로 되돌릴 수 없습니다."
        );
    }

    private static EarthTripException versionConflict(long serverVersion) {
        return new EarthTripException(
            "VERSION_CONFLICT", 409, "다른 일정 변경이 먼저 저장되었습니다.",
            Map.of("serverVersion", serverVersion)
        );
    }

    private record SnapshotItem(UUID itemId, int sortOrder, long version) { }
}
