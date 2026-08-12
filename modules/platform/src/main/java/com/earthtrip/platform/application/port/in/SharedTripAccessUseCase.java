package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SharedTripAccessUseCase {

    SharedTripResult sharedTrip(String token, String passwordSessionToken);

    PasswordSessionResult verifyPassword(String token, String password);

    record SharedTripResult(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String timeZone,
            List<String> scopes,
            List<SharedSegment> segments,
            List<SharedPlanningItem> planningItems,
            List<Map<String, Object>> reservations,
            List<SharedTotal> budgetTotals) {}

    record SharedSegment(
            String type,
            String cityName,
            String countryCode,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String transportMode,
            int sortOrder) {}

    record SharedPlanningItem(
            LocalDate localDate,
            String title,
            String status,
            int sortOrder,
            Map<String, Object> details) {}

    record SharedTotal(String currency, long amountMinor) {}

    record PasswordSessionResult(String sessionToken, Instant expiresAt) {}
}
