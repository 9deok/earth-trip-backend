package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TripShareUseCase {

    List<ShareLinkResult> list(UUID tripId, UUID actorUserId);

    ShareLinkResult create(UUID tripId, UUID actorUserId, ShareLinkCommand command);

    ShareLinkResult update(
        UUID tripId,
        UUID shareId,
        UUID actorUserId,
        ShareLinkCommand command
    );

    void revoke(UUID tripId, UUID shareId, UUID actorUserId, long baseVersion);

    SharedTripResult sharedTrip(String token, String passwordSessionToken);

    PasswordSessionResult verifyPassword(String token, String password);

    List<AccessEventResult> accessEvents(UUID tripId, UUID shareId, UUID actorUserId);

    record ShareLinkCommand(
        UUID requestId,
        String name,
        List<String> scopes,
        String password,
        Boolean removePassword,
        Instant expiresAt,
        Boolean removeExpiry,
        long baseVersion
    ) { }

    record ShareLinkResult(
        UUID shareId,
        String name,
        List<String> scopes,
        boolean passwordProtected,
        Instant expiresAt,
        String status,
        String shareToken,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }

    record SharedTripResult(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String timeZone,
        List<String> scopes,
        List<SharedSegment> segments,
        List<SharedPlanningItem> planningItems,
        List<Map<String, Object>> reservations,
        List<SharedTotal> budgetTotals
    ) { }

    record SharedSegment(
        String type,
        String cityName,
        String countryCode,
        LocalDate startDate,
        LocalDate endDate,
        String accommodationName,
        String transportMode,
        int sortOrder
    ) { }

    record SharedPlanningItem(
        LocalDate localDate,
        String title,
        String status,
        int sortOrder,
        Map<String, Object> details
    ) { }

    record SharedTotal(String currency, long amountMinor) { }

    record PasswordSessionResult(String sessionToken, Instant expiresAt) { }

    record AccessEventResult(UUID eventId, boolean success, String reason, Instant occurredAt) { }
}
