package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.route_legs;

import com.earthtrip.planning.application.port.in.RoutePlanningUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/route-legs")
class RouteLegsController {
    private final RoutePlanningUseCase useCase;
    private final CurrentActor actor;

    RouteLegsController(RoutePlanningUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @GetMapping
    List<RoutePlanningUseCase.RouteLeg> get(
            @PathVariable UUID tripId,
            @PathVariable UUID dayId,
            @RequestParam(defaultValue = "10") Integer bufferMinutes) {
        return useCase.routeLegs(tripId, dayId, actor.requireUserId(), bufferMinutes);
    }
}
