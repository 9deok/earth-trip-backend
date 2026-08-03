package com.earthtrip.planning.application.service.execution;

import com.earthtrip.planning.application.port.in.ExecutionPlanningUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.RouteOverrideUseCase;
import com.earthtrip.planning.application.port.in.RoutePreferenceUseCase;
import com.earthtrip.planning.application.port.in.ScheduleUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ExecutionPlanningService implements ExecutionPlanningUseCase {

    private final TripAccess access;
    private final TripDayUseCase days;
    private final ScheduleUseCase schedule;
    private final RoutePreferenceUseCase preferences;
    private final RouteOverrideUseCase routeOverrides;
    private final ScheduleChangeStorePort changes;
    private final Clock clock;

    ExecutionPlanningService(
        TripAccess access,
        TripDayUseCase days,
        ScheduleUseCase schedule,
        RoutePreferenceUseCase preferences,
        RouteOverrideUseCase routeOverrides,
        ScheduleChangeStorePort changes,
        Clock clock
    ) {
        this.access = access;
        this.days = days;
        this.schedule = schedule;
        this.preferences = preferences;
        this.routeOverrides = routeOverrides;
        this.changes = changes;
        this.clock = clock;
    }

    @Override
    public TodayResult today(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        TripAccess.PublicTripResult info = access.publicInfo(tripId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(info.timeZone())));
        TripDayUseCase.DayResult day = days.list(tripId, actorUserId).stream()
            .filter(candidate -> candidate.localDate().equals(today))
            .findFirst()
            .orElse(null);
        if (day == null) {
            String state = info.startDate() == null || today.isBefore(info.startDate())
                ? "NOT_STARTED"
                : "ENDED";
            return new TodayResult(state, today, null, info.timeZone(), List.of(), 0, 0);
        }
        List<PlanningResourceUseCase.ResourceResult> items = schedule.list(
            tripId, day.dayId(), actorUserId
        );
        int completedCount = (int) items.stream()
            .filter(item -> Set.of("COMPLETED", "SKIPPED").contains(item.status()))
            .count();
        return new TodayResult(
            "TRAVELING", today, day.dayId(), info.timeZone(),
            items, completedCount, items.size()
        );
    }

    @Override
    public RoutePreview routePreview(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    ) {
        List<Node> nodes = nodes(tripId, dayId, actorUserId);
        RoutePreferenceUseCase.PreferenceResult preference = preferences.get(
            tripId, actorUserId
        );
        int buffer = normalizeBuffer(bufferMinutes, preference.defaultBufferMinutes());
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides = overrides(
            tripId, dayId, actorUserId
        );
        List<Node> recommended = nearest(nodes);
        Metrics current = metrics(nodes, buffer, overrides);
        Metrics best = metrics(recommended, buffer, overrides);
        return new RoutePreview(
            dayId,
            nodes.stream().map(Node::id).toList(),
            recommended.stream().map(Node::id).toList(),
            current.distance(),
            best.distance(),
            current.minutes(),
            best.minutes(),
            buffer,
            "LOCAL_HEURISTIC",
            diagnostics(tripId, dayId, nodes, buffer, current, best, overrides)
        );
    }

    @Override
    public List<DayRouteSummary> tripRoutePreview(
        UUID tripId,
        UUID actorUserId,
        Integer bufferMinutes
    ) {
        return days.list(tripId, actorUserId).stream()
            .map(day -> {
                RoutePreview preview = routePreview(
                    tripId, day.dayId(), actorUserId, bufferMinutes
                );
                return new DayRouteSummary(
                    day.dayId(), day.localDate(), preview.currentDistanceMeters(),
                    preview.currentTravelMinutes(), preview.currentOrder()
                );
            })
            .toList();
    }

    @Override
    public RoutePreview replan(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer delayMinutes,
        boolean rain
    ) {
        List<Node> original = nodes(tripId, dayId, actorUserId);
        int delay = delayMinutes == null ? 0 : Math.max(0, delayMinutes);
        List<Node> candidates = rain
            ? original.stream()
                .filter(node -> Boolean.TRUE.equals(node.payload().get("indoor"))
                    || !Boolean.TRUE.equals(node.payload().get("weatherSensitive")))
                .toList()
            : original;
        RoutePreview preview = preview(tripId, dayId, candidates, actorUserId, null);
        List<Diagnostic> warnings = new ArrayList<>(preview.diagnostics());
        if (delay > 0) {
            warnings.add(informationalDiagnostic(
                dayId, "DELAY_APPLIED", "지연 시간을 반영한 재계획 미리보기입니다.",
                Map.of("delayMinutes", delay)
            ));
        }
        if (rain) {
            warnings.add(informationalDiagnostic(
                dayId, "RAIN_FILTER_APPLIED",
                "비에 민감한 야외 일정을 제외한 후보입니다.", Map.of()
            ));
        }
        return new RoutePreview(
            preview.dayId(), preview.currentOrder(), preview.recommendedOrder(),
            preview.currentDistanceMeters(), preview.recommendedDistanceMeters(),
            preview.currentTravelMinutes() + delay,
            preview.recommendedTravelMinutes() + delay,
            preview.bufferMinutes(), preview.source(), List.copyOf(warnings)
        );
    }

    @Override
    public List<Diagnostic> diagnostics(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    ) {
        return routePreview(tripId, dayId, actorUserId, bufferMinutes).diagnostics();
    }

    @Override
    public List<RouteLeg> routeLegs(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    ) {
        List<Node> nodes = nodes(tripId, dayId, actorUserId);
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides = overrides(
            tripId, dayId, actorUserId
        );
        String defaultMode = preferences.get(tripId, actorUserId).allowedModes().getFirst();
        List<RouteLeg> legs = new ArrayList<>();
        for (int index = 1; index < nodes.size(); index++) {
            Node from = nodes.get(index - 1);
            Node to = nodes.get(index);
            RouteOverrideUseCase.OverrideResult override = overrides.get(
                new LegKey(from.id(), to.id())
            );
            if (override == null && (!hasLocation(from) || !hasLocation(to))) {
                continue;
            }
            long meters = override != null && override.distanceMeters() != null
                ? override.distanceMeters()
                : distance(from, to);
            legs.add(new RouteLeg(
                from.id(), to.id(), meters,
                override == null ? travelMinutes(meters) : override.durationMinutes(),
                override == null ? defaultMode : override.mode(),
                override == null ? "LOCAL_HEURISTIC" : "MANUAL_OVERRIDE"
            ));
        }
        return List.copyOf(legs);
    }

    private RoutePreview preview(
        UUID tripId,
        UUID dayId,
        List<Node> nodes,
        UUID actorUserId,
        Integer requestedBufferMinutes
    ) {
        int bufferMinutes = normalizeBuffer(
            requestedBufferMinutes,
            preferences.get(tripId, actorUserId).defaultBufferMinutes()
        );
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides = overrides(
            tripId, dayId, actorUserId
        );
        List<Node> recommended = nearest(nodes);
        Metrics current = metrics(nodes, bufferMinutes, overrides);
        Metrics best = metrics(recommended, bufferMinutes, overrides);
        return new RoutePreview(
            dayId,
            nodes.stream().map(Node::id).toList(),
            recommended.stream().map(Node::id).toList(),
            current.distance(),
            best.distance(),
            current.minutes(),
            best.minutes(),
            bufferMinutes,
            "LOCAL_HEURISTIC",
            diagnostics(
                tripId, dayId, nodes, bufferMinutes, current, best, overrides
            )
        );
    }

    private List<Node> nodes(UUID tripId, UUID dayId, UUID actorUserId) {
        days.requireDay(tripId, dayId, actorUserId);
        return schedule.list(tripId, dayId, actorUserId).stream()
            .map(resource -> new Node(
                resource.resourceId(),
                number(resource.payload().get("latitude")),
                number(resource.payload().get("longitude")),
                integer(resource.payload().get("startMinute")),
                integer(resource.payload().get("endMinute")),
                integer(resource.payload().get("openingStartMinute")),
                integer(resource.payload().get("openingEndMinute")),
                Boolean.TRUE.equals(resource.payload().get("fixedTime")),
                resource.payload()
            ))
            .toList();
    }

    private static List<Node> nearest(List<Node> input) {
        if (input.size() < 3
            || input.stream().anyMatch(node -> !hasLocation(node))
            || input.stream().anyMatch(Node::fixed)) {
            return input;
        }
        List<Node> remaining = new ArrayList<>(input);
        List<Node> result = new ArrayList<>();
        Node current = remaining.removeFirst();
        result.add(current);
        while (!remaining.isEmpty()) {
            Node from = current;
            current = remaining.stream()
                .min(Comparator.comparingLong(candidate -> distance(from, candidate)))
                .orElseThrow();
            remaining.remove(current);
            result.add(current);
        }
        return List.copyOf(result);
    }

    private static Metrics metrics(
        List<Node> nodes,
        int bufferMinutes,
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides
    ) {
        long totalDistance = 0;
        int totalMinutes = 0;
        for (int index = 1; index < nodes.size(); index++) {
            Node from = nodes.get(index - 1);
            Node to = nodes.get(index);
            RouteOverrideUseCase.OverrideResult override = overrides.get(
                new LegKey(from.id(), to.id())
            );
            if (override != null) {
                totalDistance += override.distanceMeters() == null
                    ? fallbackDistance(from, to)
                    : override.distanceMeters();
                totalMinutes += override.durationMinutes() + bufferMinutes;
            } else if (hasLocation(from) && hasLocation(to)) {
                long meters = distance(from, to);
                totalDistance += meters;
                totalMinutes += travelMinutes(meters) + bufferMinutes;
            }
        }
        return new Metrics(totalDistance, totalMinutes);
    }

    private List<Diagnostic> diagnostics(
        UUID tripId,
        UUID dayId,
        List<Node> nodes,
        int bufferMinutes,
        Metrics current,
        Metrics best,
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides
    ) {
        List<RawDiagnostic> raw = new ArrayList<>();
        if (current.distance() > best.distance() * 1.25
            && current.distance() - best.distance() > 1_000) {
            raw.add(new RawDiagnostic(
                "BACKTRACKING_ROUTE", "WARNING", null,
                "되돌아가는 동선이 감지되었습니다.",
                Map.of(
                    "currentMeters", current.distance(),
                    "recommendedMeters", best.distance()
                )
            ));
        }
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (!hasLocation(node)) {
                raw.add(new RawDiagnostic(
                    "MISSING_LOCATION", "WARNING", node.id(),
                    "좌표가 없어 이동 경로를 계산할 수 없습니다.", Map.of()
                ));
            }
            if (outsideOpeningHours(node)) {
                raw.add(new RawDiagnostic(
                    "OUTSIDE_OPENING_HOURS", "ERROR", node.id(),
                    "예정 시각이 영업시간 밖입니다.",
                    Map.of(
                        "startMinute", node.start(),
                        "openingStartMinute", node.openStart(),
                        "openingEndMinute", node.openEnd()
                    )
                ));
            }
            if (index > 0) {
                addReachabilityDiagnostic(
                    raw, nodes.get(index - 1), node, bufferMinutes, overrides
                );
            }
        }
        Map<UUID, ScheduleChangeStorePort.ResolutionRecord> resolutions = new HashMap<>();
        for (ScheduleChangeStorePort.ResolutionRecord resolution
            : changes.findResolutions(tripId, dayId)) {
            resolutions.put(resolution.diagnosticId(), resolution);
        }
        return raw.stream()
            .map(item -> diagnostic(dayId, item, resolutions))
            .toList();
    }

    private static void addReachabilityDiagnostic(
        List<RawDiagnostic> diagnostics,
        Node previous,
        Node next,
        int bufferMinutes,
        Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides
    ) {
        RouteOverrideUseCase.OverrideResult override = overrides.get(
            new LegKey(previous.id(), next.id())
        );
        if (previous.end() == null || next.start() == null
            || (override == null && (!hasLocation(previous) || !hasLocation(next)))) {
            return;
        }
        int earliestArrival = previous.end()
            + (override == null
                ? travelMinutes(distance(previous, next))
                : override.durationMinutes())
            + bufferMinutes;
        if (earliestArrival > next.start()) {
            diagnostics.add(new RawDiagnostic(
                "UNREACHABLE_SCHEDULE", "ERROR", next.id(),
                "이동시간을 포함하면 예정 시각에 도착할 수 없습니다.",
                Map.of(
                    "earliestArrivalMinute", earliestArrival,
                    "scheduledMinute", next.start()
                )
            ));
        }
    }

    private static boolean outsideOpeningHours(Node node) {
        return node.start() != null
            && node.openStart() != null
            && node.openEnd() != null
            && (node.start() < node.openStart() || node.start() > node.openEnd());
    }

    private static int normalizeBuffer(Integer bufferMinutes, int defaultValue) {
        return bufferMinutes == null
            ? defaultValue
            : Math.max(0, Math.min(120, bufferMinutes));
    }

    private Map<LegKey, RouteOverrideUseCase.OverrideResult> overrides(
        UUID tripId,
        UUID dayId,
        UUID actorUserId
    ) {
        Map<LegKey, RouteOverrideUseCase.OverrideResult> result = new HashMap<>();
        for (RouteOverrideUseCase.OverrideResult override
            : routeOverrides.list(tripId, dayId, actorUserId)) {
            LegKey key = new LegKey(override.fromItemId(), override.toItemId());
            RouteOverrideUseCase.OverrideResult duplicate = result.put(key, override);
            if (duplicate != null) {
                throw new IllegalStateException("같은 일정 구간에 경로 보정이 중복 저장되었습니다.");
            }
        }
        return Map.copyOf(result);
    }

    private static Diagnostic diagnostic(
        UUID dayId,
        RawDiagnostic raw,
        Map<UUID, ScheduleChangeStorePort.ResolutionRecord> resolutions
    ) {
        UUID diagnosticId = diagnosticId(dayId, raw.code(), raw.itemId());
        ScheduleChangeStorePort.ResolutionRecord resolution = resolutions.get(diagnosticId);
        return new Diagnostic(
            diagnosticId, raw.code(), raw.severity(), raw.itemId(), raw.message(),
            raw.details(), resolution != null,
            resolution == null ? null : resolution.note(),
            resolution == null ? null : resolution.resolvedBy(),
            resolution == null ? null : resolution.resolvedAt()
        );
    }

    private static Diagnostic informationalDiagnostic(
        UUID dayId,
        String code,
        String message,
        Map<String, Object> details
    ) {
        return new Diagnostic(
            diagnosticId(dayId, code, null), code, "INFO", null, message, details,
            false, null, null, null
        );
    }

    private static UUID diagnosticId(UUID dayId, String code, UUID itemId) {
        String source = dayId + "|" + code + "|" + (itemId == null ? "DAY" : itemId);
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private static long fallbackDistance(Node from, Node to) {
        return hasLocation(from) && hasLocation(to) ? distance(from, to) : 0;
    }

    private static boolean hasLocation(Node node) {
        return node.lat() != null && node.lon() != null;
    }

    private static Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static long distance(Node from, Node to) {
        double earthRadiusMeters = 6_371_000;
        double fromLatitude = Math.toRadians(from.lat());
        double toLatitude = Math.toRadians(to.lat());
        double latitudeDelta = toLatitude - fromLatitude;
        double longitudeDelta = Math.toRadians(to.lon() - from.lon());
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
            + Math.cos(fromLatitude) * Math.cos(toLatitude)
            * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return Math.round(
            earthRadiusMeters * 2
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))
        );
    }

    private static int travelMinutes(long meters) {
        return Math.max(1, (int) Math.ceil(meters / 75.0));
    }

    private record Node(
        UUID id,
        Double lat,
        Double lon,
        Integer start,
        Integer end,
        Integer openStart,
        Integer openEnd,
        boolean fixed,
        Map<String, Object> payload
    ) { }

    private record Metrics(long distance, int minutes) { }

    private record LegKey(UUID fromItemId, UUID toItemId) { }

    private record RawDiagnostic(
        String code,
        String severity,
        UUID itemId,
        String message,
        Map<String, Object> details
    ) { }
}
