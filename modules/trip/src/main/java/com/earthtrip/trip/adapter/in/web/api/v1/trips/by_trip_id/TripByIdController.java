package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}")
class TripByIdController {

    private final TripManagementUseCase useCase;
    private final CurrentActor currentActor;

    TripByIdController(TripManagementUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase;
        this.currentActor = currentActor;
    }

    @GetMapping
    TripResponse get(@PathVariable UUID tripId) {
        return response(useCase.get(tripId, currentActor.requireUserId()));
    }

    @PatchMapping
    TripResponse patch(@PathVariable UUID tripId, @Valid @RequestBody TripUpdateRequest request) {
        return response(useCase.update(
            tripId,
            currentActor.requireUserId(),
            new TripManagementUseCase.UpdateTripCommand(
                request.title(), request.status(), request.startDate(), request.endDate(),
                request.timeZone(), request.defaultCurrency(), request.planningMode(),
                request.pace(), request.companionCount(), request.companionNames(),
                request.dateMode(), request.travelMode(), request.departurePoint(),
                request.returnPoint(), request.firstDayStartMinutes(), request.lastDayEndMinutes(),
                request.overnightTravelNights(), request.reduceStairs(), request.frequentBreaks(),
                request.walkingLimitMinutes(), request.dietaryNotes(), request.baseVersion()
            )
        ));
    }

    @DeleteMapping
    TripResponse delete(@PathVariable UUID tripId, @Valid @RequestBody TripDeleteRequest request) {
        return response(useCase.requestDeletion(
            tripId, currentActor.requireUserId(), request.baseVersion()
        ));
    }

    private static TripResponse response(TripManagementUseCase.TripResult trip) {
        return new TripResponse(
            trip.tripId(), trip.ownerUserId(), trip.title(), trip.status(), trip.startDate(),
            trip.endDate(), trip.timeZone(), trip.defaultCurrency(), trip.planningMode(), trip.pace(),
            trip.companionCount(), trip.companionNames(), trip.dateMode(), trip.travelMode(),
            trip.departurePoint(), trip.returnPoint(), trip.firstDayStartMinutes(),
            trip.lastDayEndMinutes(), trip.overnightTravelNights(), trip.reduceStairs(),
            trip.frequentBreaks(), trip.walkingLimitMinutes(), trip.dietaryNotes(),
            trip.version(), trip.createdAt(), trip.updatedAt(), trip.scheduledDeletionAt()
        );
    }
}

record TripUpdateRequest(
    String title,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    String timeZone,
    String defaultCurrency,
    String planningMode,
    String pace,
    Integer companionCount,
    java.util.List<String> companionNames,
    String dateMode,
    String travelMode,
    String departurePoint,
    String returnPoint,
    Integer firstDayStartMinutes,
    Integer lastDayEndMinutes,
    Integer overnightTravelNights,
    Boolean reduceStairs,
    Boolean frequentBreaks,
    Integer walkingLimitMinutes,
    String dietaryNotes,
    @Min(0) long baseVersion
) { }

record TripDeleteRequest(@Min(0) long baseVersion) { }

record TripResponse(
    UUID tripId,
    UUID ownerUserId,
    String title,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    String timeZone,
    String defaultCurrency,
    String planningMode,
    String pace,
    int companionCount,
    java.util.List<String> companionNames,
    String dateMode,
    String travelMode,
    String departurePoint,
    String returnPoint,
    int firstDayStartMinutes,
    int lastDayEndMinutes,
    int overnightTravelNights,
    boolean reduceStairs,
    boolean frequentBreaks,
    int walkingLimitMinutes,
    String dietaryNotes,
    long version,
    Instant createdAt,
    Instant updatedAt,
    Instant scheduledDeletionAt
) { }
