package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.bootstrap;

import com.earthtrip.platform.application.port.in.TripBootstrapUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/bootstrap")
class TripBootstrapController {

    private final TripBootstrapUseCase useCase;
    private final CurrentActor actor;

    TripBootstrapController(TripBootstrapUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    TripBootstrapResponse get(@PathVariable UUID tripId) {
        TripBootstrapUseCase.BootstrapResult result = useCase.get(
            tripId, actor.requireUserId()
        );
        return new TripBootstrapResponse(result);
    }
}

record TripBootstrapResponse(TripBootstrapUseCase.BootstrapResult data) { }
