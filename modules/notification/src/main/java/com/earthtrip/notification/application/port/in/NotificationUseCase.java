package com.earthtrip.notification.application.port.in;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationUseCase {

    List<NotificationResult> list(UUID user, boolean unreadOnly);

    void markRead(UUID user, UUID id, boolean read);

    void markManyRead(UUID user, List<UUID> ids);

    void hide(UUID user, UUID id);

    SummaryResult summary(UUID user);

    PreferenceResult preferences(UUID user);

    PreferenceResult updatePreferences(UUID user, PreferenceCommand command);

    DeviceResult registerDevice(
            UUID user, String deviceId, String platform, String token, int appBuild);

    void removeDevice(UUID user, String deviceId);

    record NotificationResult(
            UUID id,
            UUID tripId,
            String type,
            String title,
            String body,
            String deepLink,
            Map<String, Object> metadata,
            Instant createdAt,
            Instant readAt,
            long version) {}

    record SummaryResult(long totalUnread, Map<UUID, Long> byTrip, Map<String, Long> byType) {}

    record PreferenceCommand(
            Boolean mentionsEnabled,
            Boolean scheduleEnabled,
            Boolean expenseEnabled,
            Boolean invitationEnabled,
            Boolean pushEnabled,
            Boolean emailEnabled,
            LocalTime quietStart,
            LocalTime quietEnd,
            String quietTimeZone) {}

    record PreferenceResult(
            boolean mentionsEnabled,
            boolean scheduleEnabled,
            boolean expenseEnabled,
            boolean invitationEnabled,
            boolean pushEnabled,
            boolean emailEnabled,
            LocalTime quietStart,
            LocalTime quietEnd,
            String quietTimeZone,
            long version,
            Instant updatedAt) {}

    record DeviceResult(String deviceId) {}
}
