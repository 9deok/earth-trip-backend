package com.earthtrip.trip.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripSegmentUseCase {

    List<SegmentResult> list(UUID tripId, UUID actorUserId);

    SegmentResult get(UUID tripId, UUID segmentId, UUID actorUserId);

    SegmentResult create(UUID tripId, UUID actorUserId, SegmentCommand command);

    SegmentResult update(UUID tripId, UUID segmentId, UUID actorUserId, SegmentCommand command);

    void delete(UUID tripId, UUID segmentId, UUID actorUserId, long baseVersion);

    List<SegmentResult> reorder(UUID tripId, UUID actorUserId, List<OrderItem> order);

    record SegmentCommand(
            UUID requestId,
            String type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            Integer sortOrder,
            long baseVersion) {}

    record OrderItem(UUID segmentId, int sortOrder, long baseVersion) {}

    record SegmentResult(
            UUID segmentId,
            UUID tripId,
            String type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            long version,
            UUID updatedBy,
            Instant updatedAt) {}
}
