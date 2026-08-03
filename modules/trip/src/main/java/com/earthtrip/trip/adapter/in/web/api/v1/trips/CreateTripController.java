package com.earthtrip.trip.adapter.in.web.api.v1.trips;

import com.earthtrip.trip.application.port.in.CreateTripCommand;
import com.earthtrip.trip.application.port.in.CreateTripUseCase;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
class CreateTripController {

    private final CreateTripUseCase createTripUseCase;
    private final TripManagementUseCase managementUseCase;
    private final CurrentActor currentActor;

    CreateTripController(
        CreateTripUseCase createTripUseCase,
        TripManagementUseCase managementUseCase,
        CurrentActor currentActor
    ) {
        this.createTripUseCase = createTripUseCase;
        this.managementUseCase = managementUseCase;
        this.currentActor = currentActor;
    }

    @GetMapping
    List<TripResponse> get() {
        return managementUseCase.list(currentActor.requireUserId()).stream()
            .map(CreateTripController::response)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateTripResponse create(@Valid @RequestBody CreateTripRequest request) {
        return CreateTripResponse.from(
            createTripUseCase.create(new CreateTripCommand(
                request.requestId(),
                currentActor.requireUserId(),
                request.title(),
                request.timeZone(),
                request.defaultCurrency()
            ))
        );
    }

    private static TripResponse response(TripManagementUseCase.TripResult trip) {
        return new TripResponse(
            trip.tripId(), trip.title(), trip.status(), trip.startDate(), trip.endDate(),
            trip.timeZone(), trip.defaultCurrency(), trip.planningMode(), trip.pace(),
            trip.version(), trip.updatedAt(), trip.scheduledDeletionAt()
        );
    }
}

record TripResponse(
    UUID tripId,
    String title,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    String timeZone,
    String defaultCurrency,
    String planningMode,
    String pace,
    long version,
    Instant updatedAt,
    Instant scheduledDeletionAt
) { }
