package com.earthtrip.notification.application.port.out;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public final class NotificationStoreRecords {
    private NotificationStoreRecords() {}

    public record NotificationRecord(
            UUID id,
            UUID userId,
            UUID tripId,
            String type,
            String title,
            String body,
            String deepLink,
            Map<String, Object> metadata,
            Instant createdAt,
            Instant readAt,
            Instant hiddenAt,
            long version) {}

    public record NotificationSummaryRecord(
            long totalUnread, Map<UUID, Long> unreadByTrip, Map<String, Long> unreadByType) {}

    public record PreferenceRecord(
            UUID userId,
            boolean mentions,
            boolean schedule,
            boolean expense,
            boolean invitation,
            boolean push,
            boolean email,
            LocalTime quietStart,
            LocalTime quietEnd,
            String quietTimeZone,
            long version,
            Instant updatedAt) {}

    public record DeviceRecord(
            String deviceId,
            UUID userId,
            String platform,
            String tokenHash,
            String tokenCipher,
            int appBuild,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {}

    public record DeliveryAttemptRecord(
            UUID id,
            UUID notificationId,
            String deviceId,
            String status,
            int attempts,
            Instant nextAttemptAt,
            String lastError,
            String providerMessageId,
            Instant createdAt,
            Instant updatedAt) {}
}
