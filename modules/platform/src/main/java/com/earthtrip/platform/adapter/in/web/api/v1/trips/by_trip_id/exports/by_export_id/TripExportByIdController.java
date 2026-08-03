package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.exports.by_export_id;

import com.earthtrip.platform.application.port.in.TripExportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/exports/{exportId}")
class TripExportByIdController {

    private final TripExportUseCase useCase;
    private final CurrentActor actor;

    TripExportByIdController(TripExportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    TripExportUseCase.ExportResult get(
        @PathVariable UUID tripId,
        @PathVariable UUID exportId
    ) {
        return useCase.get(tripId, exportId, actor.requireUserId());
    }
}
