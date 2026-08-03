package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.exports.by_export_id.cancellations;

import com.earthtrip.platform.application.port.in.TripExportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/exports/{exportId}/cancellations")
class TripExportCancellationsController {

    private final TripExportUseCase useCase;
    private final CurrentActor actor;

    TripExportCancellationsController(TripExportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    TripExportUseCase.ExportResult cancel(
        @PathVariable UUID tripId,
        @PathVariable UUID exportId,
        @RequestParam @PositiveOrZero long baseVersion
    ) {
        return useCase.cancel(tripId, exportId, actor.requireUserId(), baseVersion);
    }
}
