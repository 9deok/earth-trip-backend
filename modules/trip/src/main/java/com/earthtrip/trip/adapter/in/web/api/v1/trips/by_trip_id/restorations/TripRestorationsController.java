package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.restorations;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/restorations")
class TripRestorationsController {

    private final TripManagementUseCase useCase;
    private final CurrentActor currentActor;

    TripRestorationsController(TripManagementUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase;
        this.currentActor = currentActor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripRestorationResponse post(
        @PathVariable UUID tripId,
        @Valid @RequestBody TripRestorationRequest request
    ) {
        TripManagementUseCase.TripResult result = useCase.restore(
            tripId, currentActor.requireUserId(), request.baseVersion()
        );
        return new TripRestorationResponse(
            result.tripId(), result.status(), result.startDate(), result.endDate(),
            result.version(), result.updatedAt()
        );
    }
}

record TripRestorationRequest(@Min(0) long baseVersion) { }

record TripRestorationResponse(
    UUID tripId,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    long version,
    Instant updatedAt
) { }
