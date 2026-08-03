package com.earthtrip.planning.application.service.schedule;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.ScheduleMoveUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ScheduleMoveService implements ScheduleMoveUseCase {

    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;
    private final PlanningResourceUseCase resources;

    ScheduleMoveService(
        TripDayUseCase days,
        ScheduleUseCase schedule,
        PlanningResourceUseCase resources
    ) {
        this.days = days;
        this.schedule = schedule;
        this.resources = resources;
    }

    @Override
    public MoveResult move(UUID tripId, UUID actorUserId, MoveCommand command) {
        if (command == null || command.itemId() == null
            || command.sourceDayId() == null || command.targetDayId() == null) {
            throw EarthTripException.badRequest(
                "INVALID_SCHEDULE_MOVE",
                "이동할 일정과 출발·도착 날짜가 필요합니다."
            );
        }
        TripDayUseCase.DayResult sourceDay = days.requireDay(
            tripId, command.sourceDayId(), actorUserId
        );
        TripDayUseCase.DayResult targetDay = days.requireDay(
            tripId, command.targetDayId(), actorUserId
        );
        List<PlanningResourceUseCase.ResourceResult> source = schedule.list(
            tripId, sourceDay.dayId(), actorUserId
        );
        PlanningResourceUseCase.ResourceResult moved = source.stream()
            .filter(item -> item.resourceId().equals(command.itemId()))
            .findFirst()
            .orElseThrow(() -> EarthTripException.notFound(
                "SCHEDULE_ITEM_NOT_FOUND",
                "출발 날짜에서 이동할 일정을 찾을 수 없습니다."
            ));
        requireVersion(moved, command.itemBaseVersion());

        if (sourceDay.dayId().equals(targetDay.dayId())) {
            applyOrder(
                tripId, actorUserId, sourceDay, source,
                normalizeOrder(command.targetOrder()), null
            );
            List<PlanningResourceUseCase.ResourceResult> ordered = schedule.list(
                tripId, sourceDay.dayId(), actorUserId
            );
            return new MoveResult(
                moved.resourceId(), sourceDay.dayId(), targetDay.dayId(), ordered, ordered
            );
        }

        List<PlanningResourceUseCase.ResourceResult> target = schedule.list(
            tripId, targetDay.dayId(), actorUserId
        );
        List<OrderItem> sourceOrder = normalizeOrder(command.sourceOrder());
        List<OrderItem> targetOrder = normalizeOrder(command.targetOrder());
        requireExactIds(
            sourceOrder,
            source.stream()
                .map(PlanningResourceUseCase.ResourceResult::resourceId)
                .filter(id -> !id.equals(moved.resourceId()))
                .collect(Collectors.toSet()),
            "출발 날짜"
        );
        Set<UUID> expectedTargetIds = target.stream()
            .map(PlanningResourceUseCase.ResourceResult::resourceId)
            .collect(Collectors.toCollection(HashSet::new));
        expectedTargetIds.add(moved.resourceId());
        requireExactIds(targetOrder, expectedTargetIds, "도착 날짜");

        applyOrder(tripId, actorUserId, sourceDay, source, sourceOrder, moved.resourceId());
        applyTargetOrder(
            tripId, actorUserId, targetDay, target, targetOrder, moved
        );
        return new MoveResult(
            moved.resourceId(), sourceDay.dayId(), targetDay.dayId(),
            schedule.list(tripId, sourceDay.dayId(), actorUserId),
            schedule.list(tripId, targetDay.dayId(), actorUserId)
        );
    }

    private void applyTargetOrder(
        UUID tripId,
        UUID actorUserId,
        TripDayUseCase.DayResult targetDay,
        List<PlanningResourceUseCase.ResourceResult> target,
        List<OrderItem> order,
        PlanningResourceUseCase.ResourceResult moved
    ) {
        Map<UUID, PlanningResourceUseCase.ResourceResult> current = target.stream()
            .collect(Collectors.toMap(
                PlanningResourceUseCase.ResourceResult::resourceId,
                Function.identity()
            ));
        for (OrderItem item : order) {
            if (item.itemId().equals(moved.resourceId())) {
                requireVersion(moved, item.baseVersion());
                resources.relocate(
                    tripId, actorUserId, "SCHEDULE_ITEM", moved.resourceId(),
                    PlanningResourceUseCase.WritePermission.EDITOR,
                    targetDay.dayId(), targetDay.localDate(), item.sortOrder(), item.baseVersion()
                );
                continue;
            }
            PlanningResourceUseCase.ResourceResult existing = current.get(item.itemId());
            requireVersion(existing, item.baseVersion());
            if (existing.sortOrder() != item.sortOrder()) {
                updateOrder(tripId, actorUserId, targetDay, existing, item);
            }
        }
    }

    private void applyOrder(
        UUID tripId,
        UUID actorUserId,
        TripDayUseCase.DayResult day,
        List<PlanningResourceUseCase.ResourceResult> currentItems,
        List<OrderItem> order,
        UUID excludedItemId
    ) {
        Set<UUID> expected = currentItems.stream()
            .map(PlanningResourceUseCase.ResourceResult::resourceId)
            .filter(id -> excludedItemId == null || !id.equals(excludedItemId))
            .collect(Collectors.toSet());
        requireExactIds(order, expected, "일정");
        Map<UUID, PlanningResourceUseCase.ResourceResult> current = currentItems.stream()
            .collect(Collectors.toMap(
                PlanningResourceUseCase.ResourceResult::resourceId,
                Function.identity()
            ));
        for (OrderItem item : order) {
            PlanningResourceUseCase.ResourceResult existing = current.get(item.itemId());
            requireVersion(existing, item.baseVersion());
            if (existing.sortOrder() != item.sortOrder()) {
                updateOrder(tripId, actorUserId, day, existing, item);
            }
        }
    }

    private void updateOrder(
        UUID tripId,
        UUID actorUserId,
        TripDayUseCase.DayResult day,
        PlanningResourceUseCase.ResourceResult existing,
        OrderItem item
    ) {
        resources.update(
            tripId, actorUserId, "SCHEDULE_ITEM", existing.resourceId(),
            PlanningResourceUseCase.WritePermission.EDITOR,
            new PlanningResourceUseCase.ResourceCommand(
                existing.resourceId(), day.dayId(), day.localDate(), null, null,
                item.sortOrder(), item.baseVersion()
            )
        );
    }

    private static List<OrderItem> normalizeOrder(List<OrderItem> order) {
        if (order == null) {
            throw EarthTripException.badRequest(
                "SCHEDULE_ORDER_REQUIRED",
                "변경 후 일정 순서가 필요합니다."
            );
        }
        List<OrderItem> safe = List.copyOf(order);
        if (safe.stream().anyMatch(java.util.Objects::isNull)
            || safe.stream().anyMatch(item -> item.itemId() == null || item.sortOrder() < 0)
            || safe.stream().map(OrderItem::itemId).distinct().count() != safe.size()
            || safe.stream().map(OrderItem::sortOrder).distinct().count() != safe.size()) {
            throw EarthTripException.badRequest(
                "INVALID_SCHEDULE_ORDER",
                "일정과 정렬 순서를 중복 없이 입력해 주세요."
            );
        }
        return safe;
    }

    private static void requireExactIds(
        List<OrderItem> order,
        Set<UUID> expected,
        String scope
    ) {
        Set<UUID> actual = order.stream().map(OrderItem::itemId).collect(Collectors.toSet());
        if (!actual.equals(expected)) {
            throw EarthTripException.badRequest(
                "INVALID_SCHEDULE_ORDER",
                scope + "의 모든 일정을 정확히 한 번 포함해야 합니다."
            );
        }
    }

    private static void requireVersion(
        PlanningResourceUseCase.ResourceResult item,
        long baseVersion
    ) {
        if (item == null || item.version() != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 일정 변경이 먼저 저장되었습니다.",
                Map.of(
                    "serverVersion", item == null ? -1 : item.version(),
                    "itemId", item == null ? "unknown" : item.resourceId()
                )
            );
        }
    }
}
