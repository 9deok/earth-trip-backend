package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.route_overrides.by_override_id;

import com.earthtrip.planning.application.port.in.RouteOverrideUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/route-overrides/{overrideId}")
class RouteOverrideByIdController {

    private final RouteOverrideUseCase useCase;
    private final CurrentActor actor;

    RouteOverrideByIdController(RouteOverrideUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @PathVariable UUID overrideId,
        @RequestParam long baseVersion
    ) {
        useCase.delete(
            tripId, dayId, overrideId, actor.requireUserId(), baseVersion
        );
    }
}
