package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExecutionPlanningUseCase {

    TodayResult today(UUID tripId, UUID actorUserId);

    RoutePreview routePreview(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    );

    List<DayRouteSummary> tripRoutePreview(
        UUID tripId,
        UUID actorUserId,
        Integer bufferMinutes
    );

    RoutePreview replan(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer delayMinutes,
        boolean rain
    );

    List<Diagnostic> diagnostics(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    );

    List<RouteLeg> routeLegs(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        Integer bufferMinutes
    );

    record TodayResult(
        String state,
        LocalDate localDate,
        UUID dayId,
        String timeZone,
        List<PlanningResourceUseCase.ResourceResult> items,
        int completedCount,
        int totalCount
    ) { }

    record RouteLeg(
        UUID fromItemId,
        UUID toItemId,
        long distanceMeters,
        int travelMinutes,
        String mode,
        String source
    ) { }

    record Diagnostic(
        UUID diagnosticId,
        String code,
        String severity,
        UUID itemId,
        String message,
        Map<String, Object> details,
        boolean resolved,
        String resolutionNote,
        UUID resolvedBy,
        Instant resolvedAt
    ) { }

    record RoutePreview(
        UUID dayId,
        List<UUID> currentOrder,
        List<UUID> recommendedOrder,
        long currentDistanceMeters,
        long recommendedDistanceMeters,
        int currentTravelMinutes,
        int recommendedTravelMinutes,
        int bufferMinutes,
        String source,
        List<Diagnostic> diagnostics
    ) { }

    record DayRouteSummary(
        UUID dayId,
        LocalDate localDate,
        long distanceMeters,
        int travelMinutes,
        List<UUID> order
    ) { }
}
