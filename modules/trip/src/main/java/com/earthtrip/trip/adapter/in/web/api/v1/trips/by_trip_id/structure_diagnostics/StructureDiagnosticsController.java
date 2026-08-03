package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.structure_diagnostics;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/structure-diagnostics")
class StructureDiagnosticsController {

    private final TripStructureUseCase useCase;
    private final CurrentActor actor;

    StructureDiagnosticsController(TripStructureUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<TripStructureUseCase.DiagnosticResult> get(@PathVariable UUID tripId) {
        return useCase.diagnostics(tripId, actor.requireUserId());
    }
}
