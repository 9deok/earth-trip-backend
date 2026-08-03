package com.earthtrip.trip.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripManagementUseCase {

    List<TripResult> list(UUID actorUserId);

    TripResult get(UUID tripId, UUID actorUserId);

    TripResult update(UUID tripId, UUID actorUserId, UpdateTripCommand command);

    TripResult requestDeletion(UUID tripId, UUID actorUserId, long baseVersion);

    TripResult restore(UUID tripId, UUID actorUserId, long baseVersion);

    TripResult copy(UUID tripId, UUID actorUserId, UUID requestId, String title);

    record UpdateTripCommand(
        String title,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String timeZone,
        String defaultCurrency,
        String planningMode,
        String pace,
        long baseVersion
    ) { }

    record TripResult(
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
        Instant createdAt,
        Instant updatedAt,
        Instant scheduledDeletionAt
    ) { }
}
