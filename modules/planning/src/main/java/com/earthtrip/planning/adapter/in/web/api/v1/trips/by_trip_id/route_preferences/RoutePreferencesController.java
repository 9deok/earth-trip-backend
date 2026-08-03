package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.route_preferences;

import com.earthtrip.planning.application.port.in.RoutePreferenceUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/route-preferences")
class RoutePreferencesController {

    private final RoutePreferenceUseCase useCase;
    private final CurrentActor actor;

    RoutePreferencesController(RoutePreferenceUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    RoutePreferenceUseCase.PreferenceResult get(@PathVariable UUID tripId) {
        return useCase.get(tripId, actor.requireUserId());
    }

    @PatchMapping
    RoutePreferenceUseCase.PreferenceResult patch(
        @PathVariable UUID tripId,
        @Valid @RequestBody RoutePreferenceRequest request
    ) {
        return useCase.update(
            tripId,
            actor.requireUserId(),
            new RoutePreferenceUseCase.PreferenceCommand(
                request.allowedModes(), request.maximumWalkingMinutes(),
                request.defaultBufferMinutes(), request.startAtAccommodation(),
                request.endAtAccommodation(), request.avoidTolls(),
                request.accessibilityRequired(), request.baseVersion()
            )
        );
    }
}

record RoutePreferenceRequest(
    @Size(min = 1, max = 4) List<String> allowedModes,
    @PositiveOrZero Integer maximumWalkingMinutes,
    @PositiveOrZero Integer defaultBufferMinutes,
    Boolean startAtAccommodation,
    Boolean endAtAccommodation,
    Boolean avoidTolls,
    Boolean accessibilityRequired,
    @PositiveOrZero long baseVersion
) { }
