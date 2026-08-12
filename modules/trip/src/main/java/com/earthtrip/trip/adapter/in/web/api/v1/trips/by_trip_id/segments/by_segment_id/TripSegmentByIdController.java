package com.earthtrip.trip.adapter.in.web.api.v1.trips.by_trip_id.segments.by_segment_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/segments/{segmentId}")
class TripSegmentByIdController {
    private final TripSegmentUseCase useCase;
    private final CurrentActor currentActor;

    TripSegmentByIdController(TripSegmentUseCase useCase, CurrentActor currentActor) {
        this.useCase = useCase;
        this.currentActor = currentActor;
    }

    @GetMapping
    TripSegmentResponse get(@PathVariable UUID tripId, @PathVariable UUID segmentId) {
        return response(useCase.get(tripId, segmentId, currentActor.requireUserId()));
    }

    @PatchMapping
    TripSegmentResponse patch(
            @PathVariable UUID tripId,
            @PathVariable UUID segmentId,
            @Valid @RequestBody TripSegmentUpdateRequest r) {
        return response(
                useCase.update(
                        tripId,
                        segmentId,
                        currentActor.requireUserId(),
                        new TripSegmentUseCase.SegmentCommand(
                                segmentId,
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
                                r.baseVersion())));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID segmentId,
            @Valid @RequestBody SegmentDeleteRequest request) {
        useCase.delete(tripId, segmentId, currentActor.requireUserId(), request.baseVersion());
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

record TripSegmentUpdateRequest(
        @NotBlank String type,
        String cityName,
        String countryCode,
        String placeId,
        BigDecimal latitude,
        BigDecimal longitude,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String accommodationName,
        String accommodationPlaceId,
        Instant checkInAt,
        Instant checkOutAt,
        String transportMode,
        Instant departureAt,
        Instant arrivalAt,
        @Min(0) Integer sortOrder,
        @Min(0) long baseVersion) {}

record SegmentDeleteRequest(@Min(0) long baseVersion) {}

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
