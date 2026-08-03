package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.structure_diagnostics.by_diagnostic_id.resolution;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/structure-diagnostics/{diagnosticId}/resolution")
class StructureDiagnosticResolutionController {

    private final TripStructureUseCase useCase;
    private final CurrentActor actor;

    StructureDiagnosticResolutionController(TripStructureUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    TripStructureUseCase.DiagnosticResult put(
        @PathVariable UUID tripId,
        @PathVariable UUID diagnosticId,
        @Valid @RequestBody StructureDiagnosticResolutionRequest request
    ) {
        return useCase.resolve(
            tripId, diagnosticId, actor.requireUserId(), request.note()
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID tripId, @PathVariable UUID diagnosticId) {
        useCase.reopenDiagnostic(tripId, diagnosticId, actor.requireUserId());
    }
}

record StructureDiagnosticResolutionRequest(@Size(max = 1000) String note) { }
