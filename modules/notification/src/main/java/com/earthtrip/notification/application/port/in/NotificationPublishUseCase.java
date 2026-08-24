package com.earthtrip.notification.application.port.in;

import java.util.Map;
import java.util.UUID;

public interface NotificationPublishUseCase {

    PublishNotificationResult publish(PublishNotificationCommand command);

    record PublishNotificationCommand(
            UUID notificationId,
            UUID userId,
            UUID tripId,
            String type,
            String title,
            String body,
            String deepLink,
            Map<String, Object> metadata) {}

    record PublishNotificationResult(
            UUID notificationId, int deliveredDevices, int failedDevices, int skippedDevices) {}
}
