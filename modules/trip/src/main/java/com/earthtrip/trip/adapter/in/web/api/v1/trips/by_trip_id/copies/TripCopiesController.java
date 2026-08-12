package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.copies;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/trips/{tripId}/copies")
class TripCopiesController {

    private final TripManagementUseCase useCase;
    private final CurrentActor currentActor;

    TripCopiesController(TripManagementUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase;
        this.currentActor = currentActor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripCopyResponse post(@PathVariable UUID tripId, @Valid @RequestBody TripCopyRequest request) {
        TripManagementUseCase.TripResult result =
                useCase.copy(
                        tripId, currentActor.requireUserId(), request.requestId(), request.title());
        return new TripCopyResponse(
                result.tripId(),
                result.title(),
                result.status(),
                result.startDate(),
                result.endDate(),
                result.version(),
                result.createdAt());
    }
}

record TripCopyRequest(@NotNull UUID requestId, String title) {}

record TripCopyResponse(
        UUID tripId,
        String title,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        long version,
        Instant createdAt) {}
