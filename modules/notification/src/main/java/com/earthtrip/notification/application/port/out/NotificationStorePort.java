package com.earthtrip.notification.application.port.out;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStorePort {

    List<NotificationRecord> list(UUID userId);

    Optional<NotificationRecord> find(UUID notificationId);

    NotificationRecord save(NotificationRecord record);

    Optional<PreferenceRecord> preference(UUID userId);

    PreferenceRecord savePreference(PreferenceRecord record);

    void saveDevice(DeviceRecord record);

    Optional<DeviceRecord> device(String deviceId);

    List<DeviceRecord> activeDevices(UUID userId);

    Optional<DeliveryAttemptRecord> deliveryAttempt(UUID notificationId, String deviceId);

    List<DeliveryAttemptRecord> dueDeliveryAttempts(Instant now, int limit);

    DeliveryAttemptRecord saveDeliveryAttempt(DeliveryAttemptRecord record);

    record NotificationRecord(
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
        long version
    ) { }

    record PreferenceRecord(
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
        Instant updatedAt
    ) { }

    record DeviceRecord(
        String deviceId,
        UUID userId,
        String platform,
        String tokenHash,
        String tokenCipher,
        int appBuild,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) { }

    record DeliveryAttemptRecord(
        UUID id,
        UUID notificationId,
        String deviceId,
        String status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        String providerMessageId,
        Instant createdAt,
        Instant updatedAt
    ) { }
}
