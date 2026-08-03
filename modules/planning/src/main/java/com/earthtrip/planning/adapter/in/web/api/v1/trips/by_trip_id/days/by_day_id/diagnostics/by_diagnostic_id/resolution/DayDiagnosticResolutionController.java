package com.earthtrip.planning.adapter.in.web.api.v1.trips.by_trip_id.days.by_day_id.diagnostics.by_diagnostic_id.resolution;

import com.earthtrip.planning.application.port.in.DayDiagnosticResolutionUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
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
@RequestMapping(
    "/api/v1/trips/{tripId}/days/{dayId}/diagnostics/{diagnosticId}/resolution"
)
class DayDiagnosticResolutionController {

    private final DayDiagnosticResolutionUseCase useCase;
    private final CurrentActor actor;

    DayDiagnosticResolutionController(
        DayDiagnosticResolutionUseCase useCase,
        CurrentActor actor
    ) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PutMapping
    DayDiagnosticResolutionUseCase.ResolutionResult put(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @PathVariable UUID diagnosticId,
        @Valid @RequestBody DayDiagnosticResolutionRequest request
    ) {
        return useCase.resolve(
            tripId, dayId, diagnosticId, actor.requireUserId(), request.note()
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @PathVariable UUID tripId,
        @PathVariable UUID dayId,
        @PathVariable UUID diagnosticId
    ) {
        useCase.reopen(tripId, dayId, diagnosticId, actor.requireUserId());
    }
}

record DayDiagnosticResolutionRequest(@Size(max = 1000) String note) { }
