package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.route_previews;

import com.earthtrip.planning.application.port.in.RoutePlanningUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/route-previews")
class DayRoutePreviewsController {
    private final RoutePlanningUseCase useCase;
    private final CurrentActor actor;

    DayRoutePreviewsController(RoutePlanningUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    RoutePlanningUseCase.RoutePreview post(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @RequestBody(required = false) RouteMutation r) {
        return useCase.routePreview(
                tripId, dayId, actor.requireUserId(), r == null ? null : r.bufferMinutes());
    }
}

record RouteMutation(Integer bufferMinutes) {}
