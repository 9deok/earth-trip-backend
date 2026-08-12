package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.search;

import com.earthtrip.platform.application.port.in.TripSearchUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/search")
class TripSearchController {

    private final TripSearchUseCase useCase;
    private final CurrentActor actor;

    TripSearchController(TripSearchUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    TripSearchUseCase.SearchResult get(
            @PathVariable UUID tripId,
            @RequestParam String query,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Integer limit) {
        return useCase.search(tripId, actor.requireUserId(), query, types, limit);
    }
}
