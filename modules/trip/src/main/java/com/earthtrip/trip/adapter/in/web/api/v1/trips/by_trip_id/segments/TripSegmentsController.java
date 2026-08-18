package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.segments;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/segments")
class TripSegmentsController {
    private final TripSegmentUseCase useCase;
    private final CurrentActor currentActor;

    TripSegmentsController(TripSegmentUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase;
        this.currentActor = currentActor;
    }

    @GetMapping
    List<TripSegmentResponse> get(@PathVariable UUID tripId) {
        return useCase.list(tripId, currentActor.requireUserId()).stream()
                .map(TripSegmentsController::response)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TripSegmentResponse post(
            @PathVariable UUID tripId, @Valid @RequestBody TripSegmentRequest request) {
        return response(useCase.create(tripId, currentActor.requireUserId(), command(request, 0)));
    }

    private static TripSegmentUseCase.SegmentCommand command(
            TripSegmentRequest r, long baseVersion) {
        return new TripSegmentUseCase.SegmentCommand(
                r.requestId(),
                r.type(),
                r.cityName(),
                r.countryCode(),
                r.placeId(),
                r.latitude(),
                r.longitude(),
                r.startDate(),
                r.endDate(),
                r.accommodationName(),
                r.accommodationPlaceId(),
                r.checkInAt(),
                r.checkOutAt(),
                r.transportMode(),
                r.departureAt(),
                r.arrivalAt(),
                r.sortOrder(),
                baseVersion);
    }

    private static TripSegmentResponse response(TripSegmentUseCase.SegmentResult s) {
        return new TripSegmentResponse(
                s.segmentId(),
                s.tripId(),
                s.type(),
                s.cityName(),
                s.countryCode(),
                s.placeId(),
                s.latitude(),
                s.longitude(),
                s.startDate(),
                s.endDate(),
                s.accommodationName(),
                s.accommodationPlaceId(),
                s.checkInAt(),
                s.checkOutAt(),
                s.transportMode(),
                s.departureAt(),
                s.arrivalAt(),
                s.sortOrder(),
                s.version(),
                s.updatedBy(),
                s.updatedAt());
    }
}

record TripSegmentRequest(
        @NotNull UUID requestId,
        @NotBlank String type,
        String cityName,
        String countryCode,
        String placeId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        String accommodationName,
        String accommodationPlaceId,
        Instant checkInAt,
        Instant checkOutAt,
        String transportMode,
        Instant departureAt,
        Instant arrivalAt,
        @Min(0) Integer sortOrder) {}

record TripSegmentResponse(
        UUID segmentId,
        UUID tripId,
        String type,
        String cityName,
        String countryCode,
        String placeId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        String accommodationName,
        String accommodationPlaceId,
        Instant checkInAt,
        Instant checkOutAt,
        String transportMode,
        Instant departureAt,
        Instant arrivalAt,
        int sortOrder,
        long version,
        UUID updatedBy,
        Instant updatedAt) {}
