package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.api.NotificationPublisher;
import com.earthtrip.notification.application.port.in.NotificationPublishUseCase;
import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.earthtrip.notification.domain.NotificationDeliveryPolicy;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class NotificationPublisherService implements NotificationPublisher, NotificationPublishUseCase {

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
    public PublishNotificationResult publish(PublishNotificationCommand command) {
        NotificationPublisher.PublishResult result =
                publish(
                        new NotificationPublisher.PublishCommand(
                                command.notificationId(),
                                command.userId(),
                                command.tripId(),
                                command.type(),
                                command.title(),
                                command.body(),
                                command.deepLink(),
                                command.metadata()));
        return new PublishNotificationResult(
                result.notificationId(),
                result.deliveredDevices(),
                result.failedDevices(),
                result.skippedDevices());
    }

    @Override
    public NotificationPublisher.PublishResult publish(
            NotificationPublisher.PublishCommand command) {
        UUID notificationId =
                command.notificationId() == null ? UUID.randomUUID() : command.notificationId();
        NotificationStoreRecords.NotificationRecord existing =
                notifications.find(notificationId).orElse(null);
        if (existing != null) {
            return new NotificationPublisher.PublishResult(
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
        if (!NotificationDeliveryPolicy.allowsPush(
                notification.type(), deliveryPreference(preference), clock.instant())) {
            return new NotificationPublisher.PublishResult(
                    notificationId, 0, 0, activeDevices.size());
        }

        PushDeliveryCoordinator.DeliverySummary summary =
                deliveries.deliver(notification, activeDevices);
        return new NotificationPublisher.PublishResult(
                notificationId, summary.delivered(), summary.failed(), 0);
    }

    private static NotificationDeliveryPolicy.Preference deliveryPreference(
            NotificationStoreRecords.PreferenceRecord preference) {
        if (preference == null) {
            return null;
        }
        return new NotificationDeliveryPolicy.Preference(
                preference.mentions(),
                preference.schedule(),
                preference.expense(),
                preference.invitation(),
                preference.push(),
                preference.quietStart(),
                preference.quietEnd(),
                preference.quietTimeZone());
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
