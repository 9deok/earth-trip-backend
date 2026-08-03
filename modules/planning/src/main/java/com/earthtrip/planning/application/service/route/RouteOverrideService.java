package com.earthtrip.planning.application.service.route;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.RouteOverrideUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RouteOverrideService implements RouteOverrideUseCase {

    private static final Set<String> MODES = Set.of(
        "WALKING", "TRANSIT", "DRIVING", "BICYCLING", "TAXI", "OTHER"
    );

    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;
    private final PlanningResourceUseCase resources;

    RouteOverrideService(
        TripDayUseCase days,
        ScheduleUseCase schedule,
        PlanningResourceUseCase resources
    ) {
        this.days = days;
        this.schedule = schedule;
        this.resources = resources;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverrideResult> list(UUID tripId, UUID dayId, UUID actorUserId) {
        TripDayUseCase.DayResult day = days.requireDay(tripId, dayId, actorUserId);
        return resources.list(tripId, actorUserId, "ROUTE_OVERRIDE", day.dayId(), day.localDate())
            .stream()
            .map(RouteOverrideService::result)
            .toList();
    }

    @Override
    public OverrideResult create(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        OverrideCommand command
    ) {
        TripDayUseCase.DayResult day = days.requireDay(tripId, dayId, actorUserId);
        if (command.requestId() == null || command.fromItemId() == null
            || command.toItemId() == null || command.fromItemId().equals(command.toItemId())) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_OVERRIDE_ITEMS",
                "서로 다른 출발 일정과 도착 일정이 필요합니다."
            );
        }
        List<PlanningResourceUseCase.ResourceResult> items = schedule.list(
            tripId, dayId, actorUserId
        );
        Set<UUID> itemIds = items.stream()
            .map(PlanningResourceUseCase.ResourceResult::resourceId)
            .collect(java.util.stream.Collectors.toSet());
        if (!itemIds.contains(command.fromItemId()) || !itemIds.contains(command.toItemId())) {
            throw EarthTripException.badRequest(
                "ROUTE_OVERRIDE_ITEM_NOT_IN_DAY",
                "같은 날짜에 있는 일정 사이에서만 경로를 보정할 수 있습니다."
            );
        }
        int duration = command.durationMinutes() == null ? -1 : command.durationMinutes();
        if (duration < 0 || duration > 1_440) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_OVERRIDE_DURATION",
                "이동시간은 0~1440분이어야 합니다."
            );
        }
        if (command.distanceMeters() != null && command.distanceMeters() < 0) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_OVERRIDE_DISTANCE",
                "이동거리는 0 이상이어야 합니다."
            );
        }
        String mode = mode(command.mode());
        String note = note(command.note());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fromItemId", command.fromItemId().toString());
        payload.put("toItemId", command.toItemId().toString());
        payload.put("durationMinutes", duration);
        if (command.distanceMeters() != null) {
            payload.put("distanceMeters", command.distanceMeters());
        }
        payload.put("mode", mode);
        if (note != null) {
            payload.put("note", note);
        }
        return result(resources.create(
            tripId, actorUserId, "ROUTE_OVERRIDE",
            PlanningResourceUseCase.WritePermission.EDITOR,
            new PlanningResourceUseCase.ResourceCommand(
                command.requestId(), dayId, day.localDate(), payload, "ACTIVE", 0, 0
            )
        ));
    }

    @Override
    public void delete(
        UUID tripId,
        UUID dayId,
        UUID overrideId,
        UUID actorUserId,
        long baseVersion
    ) {
        days.requireDay(tripId, dayId, actorUserId);
        PlanningResourceUseCase.ResourceResult override = resources.get(
            tripId, actorUserId, "ROUTE_OVERRIDE", overrideId
        );
        if (!dayId.equals(override.parentId())) {
            throw EarthTripException.notFound(
                "ROUTE_OVERRIDE_NOT_FOUND",
                "수동 경로 보정을 찾을 수 없습니다."
            );
        }
        resources.delete(
            tripId, actorUserId, "ROUTE_OVERRIDE", overrideId,
            PlanningResourceUseCase.WritePermission.EDITOR, baseVersion
        );
    }

    private static OverrideResult result(PlanningResourceUseCase.ResourceResult resource) {
        Map<String, Object> payload = resource.payload();
        return new OverrideResult(
            resource.resourceId(),
            uuid(payload.get("fromItemId")),
            uuid(payload.get("toItemId")),
            ((Number) payload.get("durationMinutes")).intValue(),
            payload.get("distanceMeters") instanceof Number value ? value.longValue() : null,
            String.valueOf(payload.get("mode")),
            payload.get("note") == null ? null : String.valueOf(payload.get("note")),
            resource.version(), resource.updatedBy(), resource.updatedAt()
        );
    }

    private static UUID uuid(Object value) {
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("저장된 경로 보정 일정 ID가 올바르지 않습니다.", exception);
        }
    }

    private static String mode(String value) {
        String normalized = value == null ? "OTHER" : value.strip().toUpperCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_OVERRIDE_MODE",
                "지원하지 않는 수동 이동수단입니다."
            );
        }
        return normalized;
    }

    private static String note(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 1_000) {
            throw EarthTripException.badRequest(
                "ROUTE_OVERRIDE_NOTE_TOO_LONG",
                "경로 보정 메모는 1000자 이하여야 합니다."
            );
        }
        return normalized;
    }
}
