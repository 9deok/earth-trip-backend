package com.earthtrip.notification.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeliveryStorePort {
    Optional<NotificationStoreRecords.DeliveryAttemptRecord> deliveryAttempt(
            UUID notificationId, String deviceId);

    List<NotificationStoreRecords.DeliveryAttemptRecord> dueDeliveryAttempts(
            Instant now, int limit);

    NotificationStoreRecords.DeliveryAttemptRecord saveDeliveryAttempt(
            NotificationStoreRecords.DeliveryAttemptRecord record);
}
