package com.earthtrip.platform.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IntegrationStorePort {

    List<ConnectionRecord> connections(UUID userId, String kind);

    Optional<ConnectionRecord> connection(UUID id);

    ConnectionRecord saveConnection(ConnectionRecord record);

    Optional<SyncRecord> sync(UUID id);

    SyncRecord saveSync(SyncRecord record);

    Optional<CalendarRecord> calendar(UUID tripId);

    CalendarRecord saveCalendar(CalendarRecord record);

    void deleteCalendar(UUID tripId);

    record ConnectionRecord(
        UUID id,
        UUID userId,
        String kind,
        String provider,
        String status,
        Set<String> scopes,
        Map<String, Object> metadata,
        String authorizationState,
        Instant authorizationExpiresAt,
        Instant lastSuccessAt,
        String errorCode,
        Instant createdAt,
        Instant updatedAt,
        Instant revokedAt,
        long version
    ) { }

    record SyncRecord(
        UUID id,
        UUID userId,
        UUID connectionId,
        UUID tripId,
        String jobType,
        String status,
        Map<String, Object> request,
        Map<String, Object> result,
        String errorCode,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }

    record CalendarRecord(
        UUID tripId,
        UUID connectionId,
        Map<String, Object> scopeConfig,
        String status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }
}
