package com.earthtrip.notification.application.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.earthtrip.notification.application.port.out.NotificationStorePort;
import com.earthtrip.notification.application.port.out.PushDeliveryPort;
import com.earthtrip.notification.application.port.out.PushTokenProtectorPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PushDeliveryCoordinatorTest {

    @Test
    void retriesTemporaryFailureAndMarksDeliveryCompleted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-04T12:00:00Z"));
        MemoryStore store = new MemoryStore();
        var notification = notification(clock.instant());
        var device = device(clock.instant());
        store.notification = notification;
        store.device = device;
        int[] calls = {0};
        PushDeliveryPort delivery = (ignoredToken, ignoredMessage) -> ++calls[0] == 1
            ? new PushDeliveryPort.DeliveryResult(
                "TEMPORARY_FAILURE", null, "FCM_HTTP_503"
            )
            : new PushDeliveryPort.DeliveryResult("DELIVERED", "message-1", null);
        PushDeliveryCoordinator coordinator = new PushDeliveryCoordinator(
            store, new PlainTokenProtector(), delivery, clock
        );

        PushDeliveryCoordinator.DeliverySummary first = coordinator.deliver(
            notification, List.of(device)
        );

        assertThat(first.failed()).isEqualTo(1);
        assertThat(store.attempt.status()).isEqualTo("RETRY");
        assertThat(store.attempt.attempts()).isEqualTo(1);
        clock.advanceSeconds(31);

        coordinator.retryDue();

        assertThat(calls[0]).isEqualTo(2);
        assertThat(store.attempt.status()).isEqualTo("DELIVERED");
        assertThat(store.attempt.attempts()).isEqualTo(2);
        assertThat(store.attempt.providerMessageId()).isEqualTo("message-1");
    }

    private static NotificationStorePort.NotificationRecord notification(Instant now) {
        return new NotificationStorePort.NotificationRecord(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SCHEDULE_CHANGED",
            "일정 변경", "일정이 바뀌었습니다.", "earthtrip://today", Map.of(),
            now, null, null, 0
        );
    }

    private static NotificationStorePort.DeviceRecord device(Instant now) {
        return new NotificationStorePort.DeviceRecord(
            "device-1", UUID.randomUUID(), "ANDROID", "hash", "token", 1,
            true, now, now
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    private static final class PlainTokenProtector implements PushTokenProtectorPort {
        @Override public ProtectedToken protect(String rawToken) {
            return new ProtectedToken(rawToken, rawToken);
        }
        @Override public String reveal(String cipherText) { return cipherText; }
    }

    private static final class MemoryStore implements NotificationStorePort {
        private NotificationRecord notification;
        private DeviceRecord device;
        private DeliveryAttemptRecord attempt;

        @Override public List<NotificationRecord> list(UUID userId) {
            return notification == null ? List.of() : List.of(notification);
        }
        @Override public Optional<NotificationRecord> find(UUID notificationId) {
            return Optional.ofNullable(notification)
                .filter(item -> item.id().equals(notificationId));
        }
        @Override public NotificationRecord save(NotificationRecord record) {
            notification = record; return record;
        }
        @Override public Optional<PreferenceRecord> preference(UUID userId) {
            return Optional.empty();
        }
        @Override public PreferenceRecord savePreference(PreferenceRecord record) {
            return record;
        }
        @Override public void saveDevice(DeviceRecord record) { device = record; }
        @Override public Optional<DeviceRecord> device(String deviceId) {
            return Optional.ofNullable(device).filter(item -> item.deviceId().equals(deviceId));
        }
        @Override public List<DeviceRecord> activeDevices(UUID userId) {
            return device == null || !device.active() ? List.of() : List.of(device);
        }
        @Override public Optional<DeliveryAttemptRecord> deliveryAttempt(
            UUID notificationId, String deviceId
        ) {
            return Optional.ofNullable(attempt).filter(item ->
                item.notificationId().equals(notificationId) && item.deviceId().equals(deviceId)
            );
        }
        @Override public List<DeliveryAttemptRecord> dueDeliveryAttempts(
            Instant now, int limit
        ) {
            List<DeliveryAttemptRecord> result = new ArrayList<>();
            if (attempt != null && attempt.status().equals("RETRY")
                && !attempt.nextAttemptAt().isAfter(now)) {
                result.add(attempt);
            }
            return result;
        }
        @Override public DeliveryAttemptRecord saveDeliveryAttempt(
            DeliveryAttemptRecord record
        ) {
            attempt = record; return record;
        }
    }
}
