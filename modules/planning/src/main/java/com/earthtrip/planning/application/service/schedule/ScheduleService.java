package com.earthtrip.planning.application.service.schedule;

import com.earthtrip.planning.application.port.in.*;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ScheduleService implements ScheduleUseCase {
    private final TripDayUseCase days;
    private final PlanningResourceUseCase resources;

    ScheduleService(TripDayUseCase d, PlanningResourceUseCase r) {
        days = d;
        resources = r;
    }

    @Override
    public List<PlanningResourceUseCase.ResourceResult> list(UUID trip, UUID day, UUID actor) {
        TripDayUseCase.DayResult d = days.requireDay(trip, day, actor);
        return resources.list(trip, actor, "SCHEDULE_ITEM", day, d.localDate());
    }

    @Override
    public PlanningResourceUseCase.ResourceResult get(UUID trip, UUID day, UUID item, UUID actor) {
        days.requireDay(trip, day, actor);
        PlanningResourceUseCase.ResourceResult r =
                resources.get(trip, actor, "SCHEDULE_ITEM", item);
        parent(r, day);
        return r;
    }

    @Override
    public PlanningResourceUseCase.ResourceResult create(
            UUID trip, UUID day, UUID actor, PlanningResourceUseCase.ResourceCommand c) {
        TripDayUseCase.DayResult d = days.requireDay(trip, day, actor);
        return resources.create(
                trip,
                actor,
                "SCHEDULE_ITEM",
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        c.requestId(),
                        day,
                        d.localDate(),
                        c.payload(),
                        c.status(),
                        c.sortOrder(),
                        0));
    }

    @Override
    public PlanningResourceUseCase.ResourceResult update(
            UUID trip, UUID day, UUID item, UUID actor, PlanningResourceUseCase.ResourceCommand c) {
        TripDayUseCase.DayResult d = days.requireDay(trip, day, actor);
        parent(resources.get(trip, actor, "SCHEDULE_ITEM", item), day);
        return resources.update(
                trip,
                actor,
                "SCHEDULE_ITEM",
                item,
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                        item,
                        day,
                        d.localDate(),
                        c.payload(),
                        c.status(),
                        c.sortOrder(),
                        c.baseVersion()));
    }

    @Override
    public void delete(UUID trip, UUID day, UUID item, UUID actor, long v) {
        days.requireDay(trip, day, actor);
        parent(resources.get(trip, actor, "SCHEDULE_ITEM", item), day);
        resources.delete(
                trip,
                actor,
                "SCHEDULE_ITEM",
                item,
                PlanningResourceUseCase.WritePermission.EDITOR,
                v);
    }

    @Override
    public List<PlanningResourceUseCase.ResourceResult> reorder(
            UUID trip, UUID day, UUID actor, List<OrderItem> items) {
        List<PlanningResourceUseCase.ResourceResult> current = list(trip, day, actor);
        if (items.size() != current.size()
                || items.stream().map(OrderItem::itemId).distinct().count() != items.size()
                || items.stream().map(OrderItem::sortOrder).distinct().count() != items.size())
            throw EarthTripException.badRequest("INVALID_SCHEDULE_ORDER", "모든 일정을 중복 없이 포함해야 합니다.");
        for (OrderItem i : items) {
            PlanningResourceUseCase.ResourceResult old =
                    current.stream()
                            .filter(r -> r.resourceId().equals(i.itemId()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            EarthTripException.badRequest(
                                                    "INVALID_SCHEDULE_ORDER",
                                                    "다른 날짜의 일정이 포함됐습니다."));
            resources.update(
                    trip,
                    actor,
                    "SCHEDULE_ITEM",
                    i.itemId(),
                    PlanningResourceUseCase.WritePermission.EDITOR,
                    new PlanningResourceUseCase.ResourceCommand(
                            i.itemId(),
                            day,
                            old.localDate(),
                            null,
                            null,
                            i.sortOrder(),
                            i.baseVersion()));
        }
        return list(trip, day, actor);
    }

    private static void parent(PlanningResourceUseCase.ResourceResult r, UUID day) {
        if (!Objects.equals(r.parentId(), day))
            throw EarthTripException.notFound("SCHEDULE_ITEM_NOT_FOUND", "일정을 찾을 수 없습니다.");
    }
}
