package com.earthtrip.notification.api;

import java.util.Map;
import java.util.UUID;

public interface NotificationPublisher {

    PublishResult publish(PublishCommand command);

    record PublishCommand(
        UUID notificationId,
        UUID userId,
        UUID tripId,
        String type,
        String title,
        String body,
        String deepLink,
        Map<String, Object> metadata
    ) { }

    record PublishResult(
        UUID notificationId,
        int deliveredDevices,
        int failedDevices,
        int skippedDevices
    ) { }
}
