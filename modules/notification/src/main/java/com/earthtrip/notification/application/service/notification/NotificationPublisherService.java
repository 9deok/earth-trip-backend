package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.api.NotificationPublisher;
import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class NotificationPublisherService implements NotificationPublisher {

    private final NotificationRecordStorePort notifications;
    private final NotificationPreferenceStorePort preferences;
    private final PushDeviceStorePort devices;
    private final PushDeliveryCoordinator deliveries;
    private final Clock clock;

    NotificationPublisherService(
            NotificationRecordStorePort notifications,
            NotificationPreferenceStorePort preferences,
            PushDeviceStorePort devices,
            PushDeliveryCoordinator deliveries,
            Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.devices = devices;
        this.deliveries = deliveries;
        this.clock = clock;
    }

    @Override
    public PublishResult publish(PublishCommand command) {
        UUID notificationId =
                command.notificationId() == null ? UUID.randomUUID() : command.notificationId();
        NotificationStoreRecords.NotificationRecord existing =
                notifications.find(notificationId).orElse(null);
        if (existing != null) {
            return new PublishResult(
                    notificationId, 0, 0, devices.activeDevices(command.userId()).size());
        }

        NotificationStoreRecords.NotificationRecord notification =
                notifications.save(
                        new NotificationStoreRecords.NotificationRecord(
                                notificationId,
                                command.userId(),
                                command.tripId(),
                                normalizedType(command.type()),
                                requiredText(command.title(), "알림 제목"),
                                requiredText(command.body(), "알림 본문"),
                                command.deepLink(),
                                command.metadata() == null
                                        ? Map.of()
                                        : Map.copyOf(command.metadata()),
                                clock.instant(),
                                null,
                                null,
                                0));

        NotificationStoreRecords.PreferenceRecord preference =
                preferences.preference(command.userId()).orElse(null);
        var activeDevices = devices.activeDevices(command.userId());
        if (!pushAllowed(preference, command.type()) || inQuietHours(preference)) {
            return new PublishResult(notificationId, 0, 0, activeDevices.size());
        }

        PushDeliveryCoordinator.DeliverySummary summary =
                deliveries.deliver(notification, activeDevices);
        return new PublishResult(notificationId, summary.delivered(), summary.failed(), 0);
    }

    private static boolean pushAllowed(
            NotificationStoreRecords.PreferenceRecord preference, String type) {
        if (preference == null) {
            return true;
        }
        if (!preference.push()) {
            return false;
        }
        String normalized = normalizedType(type);
        if (normalized.contains("MENTION")) {
            return preference.mentions();
        }
        if (normalized.contains("SCHEDULE") || normalized.contains("CALENDAR")) {
            return preference.schedule();
        }
        if (normalized.contains("EXPENSE") || normalized.contains("WALLET")) {
            return preference.expense();
        }
        if (normalized.contains("INVITATION") || normalized.contains("INVITE")) {
            return preference.invitation();
        }
        return true;
    }

    private boolean inQuietHours(NotificationStoreRecords.PreferenceRecord preference) {
        if (preference == null
                || preference.quietStart() == null
                || preference.quietEnd() == null) {
            return false;
        }
        ZoneId zone = ZoneId.of(preference.quietTimeZone());
        LocalTime now = ZonedDateTime.ofInstant(clock.instant(), zone).toLocalTime();
        LocalTime start = preference.quietStart();
        LocalTime end = preference.quietEnd();
        if (start.equals(end)) {
            return true;
        }
        return start.isBefore(end)
                ? !now.isBefore(start) && now.isBefore(end)
                : !now.isBefore(start) || now.isBefore(end);
    }

    private static String normalizedType(String type) {
        return requiredText(type, "알림 유형").toUpperCase(Locale.ROOT);
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.strip();
    }
}
