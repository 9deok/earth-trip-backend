package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.replan_previews;

import com.earthtrip.planning.application.port.in.RoutePlanningUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/replan-previews")
class ReplanPreviewsController {
    private final RoutePlanningUseCase useCase;
    private final CurrentActor actor;

    ReplanPreviewsController(RoutePlanningUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PostMapping
    RoutePlanningUseCase.RoutePreview post(
            @PathVariable UUID tripId, @PathVariable UUID dayId, @RequestBody ReplanMutation r) {
        return useCase.replan(tripId, dayId, actor.requireUserId(), r.delayMinutes(), r.rain());
    }
}

record ReplanMutation(Integer delayMinutes, boolean rain) {}
