package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeliveryPort;
import com.earthtrip.notification.application.port.out.PushDeliveryStorePort;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.earthtrip.notification.application.port.out.PushTokenProtectorPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PushDeliveryCoordinator {

    private static final int MAX_ATTEMPTS = 6;
    private static final int RETRY_BATCH_SIZE = 100;
    private static final List<Duration> BACKOFF =
            List.of(
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(2),
                    Duration.ofMinutes(10),
                    Duration.ofHours(1),
                    Duration.ofHours(6));

    private final NotificationRecordStorePort notifications;
    private final PushDeviceStorePort devices;
    private final PushDeliveryStorePort attempts;
    private final PushTokenProtectorPort protector;
    private final PushDeliveryPort delivery;
    private final Clock clock;

    PushDeliveryCoordinator(
            NotificationRecordStorePort notifications,
            PushDeviceStorePort devices,
            PushDeliveryStorePort attempts,
            PushTokenProtectorPort protector,
            PushDeliveryPort delivery,
            Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.attempts = attempts;
        this.protector = protector;
        this.delivery = delivery;
        this.clock = clock;
    }

    DeliverySummary deliver(
            NotificationStoreRecords.NotificationRecord notification,
            List<NotificationStoreRecords.DeviceRecord> devices) {
        int delivered = 0;
        int failed = 0;
        for (NotificationStoreRecords.DeviceRecord device : devices) {
            DeliveryOutcome outcome = deliverOne(notification, device, null);
            if (outcome == DeliveryOutcome.DELIVERED) {
                delivered++;
            } else {
                failed++;
            }
        }
        return new DeliverySummary(delivered, failed);
    }

    @Scheduled(fixedDelayString = "${earthtrip.push.retry-delay-ms:30000}")
    public void retryDue() {
        Instant now = clock.instant();
        for (NotificationStoreRecords.DeliveryAttemptRecord attempt :
                attempts.dueDeliveryAttempts(now, RETRY_BATCH_SIZE)) {
            NotificationStoreRecords.NotificationRecord notification =
                    notifications.find(attempt.notificationId()).orElse(null);
            NotificationStoreRecords.DeviceRecord device =
                    devices.device(attempt.deviceId())
                            .filter(NotificationStoreRecords.DeviceRecord::active)
                            .orElse(null);
            if (notification == null || device == null) {
                attempts.saveDeliveryAttempt(
                        finished(attempt, "FAILED", "DELIVERY_TARGET_UNAVAILABLE", null, now));
                continue;
            }
            deliverOne(notification, device, attempt);
        }
    }

    private DeliveryOutcome deliverOne(
            NotificationStoreRecords.NotificationRecord notification,
            NotificationStoreRecords.DeviceRecord device,
            NotificationStoreRecords.DeliveryAttemptRecord existing) {
        Instant now = clock.instant();
        NotificationStoreRecords.DeliveryAttemptRecord attempt =
                existing == null
                        ? attempts.deliveryAttempt(notification.id(), device.deviceId())
                                .orElseGet(
                                        () ->
                                                new NotificationStoreRecords.DeliveryAttemptRecord(
                                                        UUID.randomUUID(),
                                                        notification.id(),
                                                        device.deviceId(),
                                                        "PENDING",
                                                        0,
                                                        null,
                                                        null,
                                                        null,
                                                        now,
                                                        now))
                        : existing;
        if (attempt.status().equals("DELIVERED")) {
            return DeliveryOutcome.DELIVERED;
        }
        int attemptNumber = attempt.attempts() + 1;
        try {
            PushDeliveryPort.DeliveryResult result =
                    delivery.send(protector.reveal(device.tokenCipher()), message(notification));
            if (result.status().equals("DELIVERED")) {
                attempts.saveDeliveryAttempt(
                        new NotificationStoreRecords.DeliveryAttemptRecord(
                                attempt.id(),
                                attempt.notificationId(),
                                attempt.deviceId(),
                                "DELIVERED",
                                attemptNumber,
                                null,
                                null,
                                result.providerMessageId(),
                                attempt.createdAt(),
                                now));
                return DeliveryOutcome.DELIVERED;
            }
            if (result.invalidToken()) {
                deactivate(device, now);
                attempts.saveDeliveryAttempt(
                        finished(
                                attempt,
                                "FAILED",
                                result.errorCode(),
                                result.providerMessageId(),
                                now,
                                attemptNumber));
                return DeliveryOutcome.FAILED;
            }
            if (result.status().equals("TEMPORARY_FAILURE")) {
                retry(attempt, attemptNumber, result.errorCode(), now);
                return DeliveryOutcome.RETRY;
            }
            attempts.saveDeliveryAttempt(
                    finished(
                            attempt,
                            "FAILED",
                            result.errorCode(),
                            result.providerMessageId(),
                            now,
                            attemptNumber));
            return DeliveryOutcome.FAILED;
        } catch (RuntimeException exception) {
            retry(attempt, attemptNumber, exception.getClass().getSimpleName(), now);
            return DeliveryOutcome.RETRY;
        }
    }

    private void retry(
            NotificationStoreRecords.DeliveryAttemptRecord attempt,
            int attemptNumber,
            String error,
            Instant now) {
        if (attemptNumber >= MAX_ATTEMPTS) {
            attempts.saveDeliveryAttempt(
                    finished(attempt, "FAILED", error, null, now, attemptNumber));
            return;
        }
        Duration delay = BACKOFF.get(Math.min(attemptNumber - 1, BACKOFF.size() - 1));
        attempts.saveDeliveryAttempt(
                new NotificationStoreRecords.DeliveryAttemptRecord(
                        attempt.id(),
                        attempt.notificationId(),
                        attempt.deviceId(),
                        "RETRY",
                        attemptNumber,
                        now.plus(delay),
                        safeError(error),
                        null,
                        attempt.createdAt(),
                        now));
    }

    private static NotificationStoreRecords.DeliveryAttemptRecord finished(
            NotificationStoreRecords.DeliveryAttemptRecord attempt,
            String status,
            String error,
            String providerMessageId,
            Instant now) {
        return finished(attempt, status, error, providerMessageId, now, attempt.attempts() + 1);
    }

    private static NotificationStoreRecords.DeliveryAttemptRecord finished(
            NotificationStoreRecords.DeliveryAttemptRecord attempt,
            String status,
            String error,
            String providerMessageId,
            Instant now,
            int attempts) {
        return new NotificationStoreRecords.DeliveryAttemptRecord(
                attempt.id(),
                attempt.notificationId(),
                attempt.deviceId(),
                status,
                attempts,
                null,
                safeError(error),
                providerMessageId,
                attempt.createdAt(),
                now);
    }

    private void deactivate(NotificationStoreRecords.DeviceRecord device, Instant now) {
        devices.saveDevice(
                new NotificationStoreRecords.DeviceRecord(
                        device.deviceId(),
                        device.userId(),
                        device.platform(),
                        device.tokenHash(),
                        device.tokenCipher(),
                        device.appBuild(),
                        false,
                        device.createdAt(),
                        now));
    }

    private static PushDeliveryPort.PushMessage message(
            NotificationStoreRecords.NotificationRecord notification) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notification_id", notification.id().toString());
        data.put("type", notification.type());
        if (notification.tripId() != null) {
            data.put("trip_id", notification.tripId().toString());
        }
        notification
                .metadata()
                .forEach(
                        (key, value) -> {
                            if (key != null && value != null && scalar(value)) {
                                data.put(key, String.valueOf(value));
                            }
                        });
        return new PushDeliveryPort.PushMessage(
                notification.title(),
                notification.body(),
                notification.deepLink(),
                Map.copyOf(data));
    }

    private static boolean scalar(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof UUID;
    }

    private static String safeError(String value) {
        if (value == null || value.isBlank()) {
            return "PUSH_DELIVERY_FAILED";
        }
        String normalized = value.strip();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    record DeliverySummary(int delivered, int failed) {}

    private enum DeliveryOutcome {
        DELIVERED,
        RETRY,
        FAILED
    }
}
