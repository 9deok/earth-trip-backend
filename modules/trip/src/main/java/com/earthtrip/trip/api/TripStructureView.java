package com.earthtrip.trip.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripStructureView {

    StructureSnapshot snapshot(UUID tripId, UUID actorUserId);

    record StructureSnapshot(Trip trip, List<Segment> segments) { }

    record Trip(
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
        long version,
        Instant updatedAt
    ) { }

    record Segment(
        UUID segmentId,
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
        long version
    ) { }
}
