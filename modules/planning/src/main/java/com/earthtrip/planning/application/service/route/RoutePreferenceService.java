package com.earthtrip.planning.application.service.route;

import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.RoutePreferenceUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
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
class RoutePreferenceService implements RoutePreferenceUseCase {

    private static final Set<String> MODES = Set.of(
        "WALKING", "TRANSIT", "DRIVING", "BICYCLING"
    );

    private final PlanningResourceUseCase resources;

    RoutePreferenceService(PlanningResourceUseCase resources) {
        this.resources = resources;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenceResult get(UUID tripId, UUID actorUserId) {
        PlanningResourceUseCase.ResourceResult resource = current(tripId, actorUserId);
        return resource == null ? defaults() : result(resource);
    }

    @Override
    public PreferenceResult update(
        UUID tripId,
        UUID actorUserId,
        PreferenceCommand command
    ) {
        PlanningResourceUseCase.ResourceResult current = current(tripId, actorUserId);
        PreferenceResult before = current == null ? defaults() : result(current);
        if (current == null && command.baseVersion() != 0) {
            throw versionConflict(0);
        }
        List<String> modes = command.allowedModes() == null
            ? before.allowedModes()
            : modes(command.allowedModes());
        int maximumWalking = number(
            command.maximumWalkingMinutes(), before.maximumWalkingMinutes(), 0, 240,
            "INVALID_MAXIMUM_WALKING_MINUTES", "최대 도보 시간은 0~240분이어야 합니다."
        );
        int buffer = number(
            command.defaultBufferMinutes(), before.defaultBufferMinutes(), 0, 120,
            "INVALID_DEFAULT_BUFFER_MINUTES", "기본 완충시간은 0~120분이어야 합니다."
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("allowedModes", modes);
        payload.put("maximumWalkingMinutes", maximumWalking);
        payload.put("defaultBufferMinutes", buffer);
        payload.put("startAtAccommodation", value(
            command.startAtAccommodation(), before.startAtAccommodation()
        ));
        payload.put("endAtAccommodation", value(
            command.endAtAccommodation(), before.endAtAccommodation()
        ));
        payload.put("avoidTolls", value(command.avoidTolls(), before.avoidTolls()));
        payload.put("accessibilityRequired", value(
            command.accessibilityRequired(), before.accessibilityRequired()
        ));

        PlanningResourceUseCase.ResourceResult saved;
        if (current == null) {
            saved = resources.create(
                tripId, actorUserId, "ROUTE_PREFERENCE",
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                    preferenceId(tripId), null, null, payload, "ACTIVE", 0, 0
                )
            );
        } else {
            saved = resources.update(
                tripId, actorUserId, "ROUTE_PREFERENCE", current.resourceId(),
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                    current.resourceId(), null, null, payload, null, null,
                    command.baseVersion()
                )
            );
        }
        return result(saved);
    }

    private PlanningResourceUseCase.ResourceResult current(UUID tripId, UUID actorUserId) {
        List<PlanningResourceUseCase.ResourceResult> all = resources.list(
            tripId, actorUserId, "ROUTE_PREFERENCE", null, null
        );
        if (all.size() > 1) {
            throw EarthTripException.conflict(
                "DUPLICATE_ROUTE_PREFERENCES",
                "여행 경로 설정이 중복되어 관리자 확인이 필요합니다."
            );
        }
        return all.isEmpty() ? null : all.getFirst();
    }

    private static PreferenceResult result(PlanningResourceUseCase.ResourceResult resource) {
        Map<String, Object> payload = resource.payload();
        return new PreferenceResult(
            modes(payload.get("allowedModes")),
            integer(payload.get("maximumWalkingMinutes"), 30),
            integer(payload.get("defaultBufferMinutes"), 10),
            bool(payload.get("startAtAccommodation"), true),
            bool(payload.get("endAtAccommodation"), true),
            bool(payload.get("avoidTolls"), false),
            bool(payload.get("accessibilityRequired"), false),
            resource.version()
        );
    }

    private static PreferenceResult defaults() {
        return new PreferenceResult(
            List.of("WALKING", "TRANSIT"), 30, 10, true, true, false, false, 0
        );
    }

    private static List<String> modes(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of("WALKING", "TRANSIT");
        }
        return modes(collection.stream().map(String::valueOf).toList());
    }

    private static List<String> modes(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw EarthTripException.badRequest(
                "ROUTE_MODE_REQUIRED",
                "허용 이동수단을 하나 이상 선택해 주세요."
            );
        }
        List<String> normalized = values.stream()
            .map(value -> value.strip().toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
        if (!MODES.containsAll(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_ROUTE_MODE",
                "지원하지 않는 이동수단이 포함되어 있습니다."
            );
        }
        return normalized;
    }

    private static int number(
        Integer value,
        int fallback,
        int minimum,
        int maximum,
        String code,
        String message
    ) {
        int resolved = value == null ? fallback : value;
        if (resolved < minimum || resolved > maximum) {
            throw EarthTripException.badRequest(code, message);
        }
        return resolved;
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    private static boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private static UUID preferenceId(UUID tripId) {
        return UUID.nameUUIDFromBytes(
            ("earthtrip:route-preferences:" + tripId).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static EarthTripException versionConflict(long serverVersion) {
        return new EarthTripException(
            "VERSION_CONFLICT", 409, "다른 경로 설정 변경이 먼저 저장되었습니다.",
            Map.of("serverVersion", serverVersion)
        );
    }
}
