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
        Integer companionCount,
        List<String> companionNames,
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
        long baseVersion
    ) {
        public UpdateTripCommand(
            String title, String status, LocalDate startDate, LocalDate endDate,
            String timeZone, String defaultCurrency, String planningMode,
            String pace, long baseVersion
        ) {
            this(
                title, status, startDate, endDate, timeZone, defaultCurrency,
                planningMode, pace, null, null, null, null, null, null,
                null, null, null, null, null, null, null, baseVersion
            );
        }
    }

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
        int companionCount,
        List<String> companionNames,
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
}
