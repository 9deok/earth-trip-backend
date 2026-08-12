package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.share_links.by_share_id.access_events;

import com.earthtrip.platform.application.port.in.TripShareManagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/share-links/{shareId}/access-events")
class ShareAccessEventsController {

    private final TripShareManagementUseCase useCase;
    private final CurrentActor actor;

    ShareAccessEventsController(TripShareManagementUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<TripShareManagementUseCase.AccessEventResult> get(
            @PathVariable UUID tripId, @PathVariable UUID shareId) {
        return useCase.accessEvents(tripId, shareId, actor.requireUserId());
    }
}
