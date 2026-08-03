package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.route_overrides;

import com.earthtrip.planning.application.port.in.RouteOverrideUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/days/{dayId}/route-overrides")
class RouteOverridesController {

    private final RouteOverrideUseCase useCase;
    private final CurrentActor actor;

    RouteOverridesController(RouteOverrideUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<RouteOverrideUseCase.OverrideResult> get(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId
    ) {
        return useCase.list(tripId, dayId, actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RouteOverrideUseCase.OverrideResult post(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @Valid @RequestBody RouteOverrideRequest request
    ) {
        return useCase.create(
            tripId, dayId, actor.requireUserId(),
            new RouteOverrideUseCase.OverrideCommand(
                request.requestId(), request.fromItemId(), request.toItemId(),
                request.durationMinutes(), request.distanceMeters(), request.mode(), request.note()
            )
        );
    }
}

record RouteOverrideRequest(
    @NotNull UUID requestId,
    @NotNull UUID fromItemId,
    @NotNull UUID toItemId,
    @NotNull @PositiveOrZero Integer durationMinutes,
    @PositiveOrZero Long distanceMeters,
    String mode,
    @Size(max = 1000) String note
) { }
