package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.route_previews;

import com.earthtrip.planning.application.port.in.RoutePlanningUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/route-previews")
class TripRoutePreviewsController {
    private final RoutePlanningUseCase useCase;
    private final CurrentActor actor;

    TripRoutePreviewsController(RoutePlanningUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    List<RoutePlanningUseCase.DayRouteSummary> post(
            @PathVariable UUID tripId, @RequestBody(required = false) RouteMutation r) {
        return useCase.tripRoutePreview(
                tripId, actor.requireUserId(), r == null ? null : r.bufferMinutes());
    }
}

record RouteMutation(Integer bufferMinutes) {}
