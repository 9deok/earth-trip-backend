package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.exports;

import com.earthtrip.platform.application.port.in.TripExportUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/exports")
class TripExportsController {

    private final TripExportUseCase useCase;
    private final CurrentActor actor;

    TripExportsController(TripExportUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    TripExportUseCase.ExportResult create(
        @PathVariable UUID tripId,
        @Valid @RequestBody TripExportRequest request
    ) {
        return useCase.create(
            tripId, actor.requireUserId(),
            new TripExportUseCase.ExportCommand(
                request.requestId(), request.format(), request.scopes()
            )
        );
    }
}

record TripExportRequest(
    @NotNull UUID requestId,
    @NotBlank String format,
    Set<String> scopes
) { }
