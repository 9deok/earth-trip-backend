package com.earthtrip.platform.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ExternalAccountProviderPort {

    boolean supports(String provider);

    boolean configured();

    AuthorizationResult authorize(AuthorizationCommand command);

    void revoke(Map<String, Object> protectedMetadata);

    ConnectionCheckResult checkConnection(Map<String, Object> protectedMetadata);

    CalendarSyncResult syncCalendar(CalendarSyncCommand command);

    record AuthorizationCommand(
        String authorizationCode,
        String redirectUri,
        String codeVerifier,
        Set<String> requestedScopes
    ) { }

    record AuthorizationResult(
        Set<String> grantedScopes,
        Map<String, Object> protectedMetadata
    ) { }

    record ConnectionCheckResult(String status, Map<String, Object> details) { }

    record CalendarSyncCommand(
        UUID tripId,
        String tripTitle,
        String defaultTimeZone,
        Map<String, Object> protectedMetadata,
        Map<String, Object> scopeConfig,
        List<CalendarEvent> events
    ) { }

    record CalendarEvent(
        UUID sourceId,
        LocalDate localDate,
        String title,
        String description,
        String location,
        String startDateTime,
        String endDateTime,
        String timeZone
    ) { }

    record CalendarSyncResult(
        Map<String, Object> scopeConfig,
        int created,
        int updated,
        int deleted
    ) { }
}
