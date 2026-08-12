package com.earthtrip.notification.adapter.out.persistence.notification;

import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords.DeliveryAttemptRecord;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords.DeviceRecord;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords.NotificationRecord;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords.NotificationSummaryRecord;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords.PreferenceRecord;
import com.earthtrip.notification.application.port.out.PushDeliveryStorePort;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class NotificationPersistenceAdapter
        implements NotificationRecordStorePort,
                NotificationPreferenceStorePort,
                PushDeviceStorePort,
                PushDeliveryStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final NotificationJpaRepository notifications;
    private final NotificationPreferenceJpaRepository preferences;
    private final PushDeviceJpaRepository devices;
    private final PushDeliveryAttemptJpaRepository deliveryAttempts;
    private final ObjectMapper json;

    NotificationPersistenceAdapter(
            NotificationJpaRepository notifications,
            NotificationPreferenceJpaRepository preferences,
            PushDeviceJpaRepository devices,
            PushDeliveryAttemptJpaRepository deliveryAttempts,
            ObjectMapper json) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.devices = devices;
        this.deliveryAttempts = deliveryAttempts;
        this.json = json;
    }

    @Override
    public List<NotificationRecord> list(UUID userId) {
        return notifications
                .findAllByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(userId.toString())
                .stream()
                .map(this::record)
                .toList();
    }

    @Override
    public Optional<NotificationRecord> find(UUID notificationId) {
        return notifications.findById(notificationId.toString()).map(this::record);
    }

    @Override
    public NotificationRecord save(NotificationRecord record) {
        String metadata = write(record.metadata());
        NotificationJpaEntity entity =
                notifications
                        .findById(record.id().toString())
                        .orElseGet(NotificationJpaEntity::new);
        entity.apply(record, metadata);
        return record(notifications.saveAndFlush(entity));
    }

    @Override
    public int markManyRead(UUID userId, List<UUID> notificationIds, java.time.Instant readAt) {
        if (notificationIds.isEmpty()) return 0;
        return notifications.markManyRead(
                userId.toString(), notificationIds.stream().map(UUID::toString).toList(), readAt);
    }

    @Override
    public NotificationSummaryRecord summary(UUID userId) {
        String id = userId.toString();
        Map<UUID, Long> byTrip = new LinkedHashMap<>();
        notifications
                .unreadByTrip(id)
                .forEach(row -> byTrip.put(UUID.fromString(row.getKey()), row.getTotal()));
        Map<String, Long> byType = new LinkedHashMap<>();
        notifications.unreadByType(id).forEach(row -> byType.put(row.getKey(), row.getTotal()));
        return new NotificationSummaryRecord(
                notifications.countByUserIdAndHiddenAtIsNullAndReadAtIsNull(id),
                Map.copyOf(byTrip),
                Map.copyOf(byType));
    }

    @Override
    public Optional<PreferenceRecord> preference(UUID userId) {
        return preferences.findById(userId.toString()).map(NotificationPreferenceJpaEntity::record);
    }

    @Override
    public PreferenceRecord savePreference(PreferenceRecord record) {
        NotificationPreferenceJpaEntity entity =
                preferences
                        .findById(record.userId().toString())
                        .orElseGet(NotificationPreferenceJpaEntity::new);
        entity.apply(record);
        return preferences.saveAndFlush(entity).record();
    }

    @Override
    public void saveDevice(DeviceRecord record) {
        PushDeviceJpaEntity entity =
                devices.findById(record.deviceId()).orElseGet(PushDeviceJpaEntity::new);
        entity.apply(record);
        devices.save(entity);
    }

    @Override
    public Optional<DeviceRecord> device(String deviceId) {
        return devices.findById(deviceId).map(PushDeviceJpaEntity::record);
    }

    @Override
    public Optional<DeviceRecord> deviceByTokenHash(String tokenHash) {
        return devices.findByTokenHash(tokenHash).map(PushDeviceJpaEntity::record);
    }

    @Override
    public List<DeviceRecord> activeDevices(UUID userId) {
        return devices.findAllByUserIdAndActiveTrue(userId.toString()).stream()
                .map(PushDeviceJpaEntity::record)
                .toList();
    }

    @Override
    public Optional<DeliveryAttemptRecord> deliveryAttempt(UUID notificationId, String deviceId) {
        return deliveryAttempts
                .findByNotificationIdAndDeviceId(notificationId.toString(), deviceId)
                .map(PushDeliveryAttemptJpaEntity::record);
    }

    @Override
    public List<DeliveryAttemptRecord> dueDeliveryAttempts(java.time.Instant now, int limit) {
        return deliveryAttempts
                .findAllByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        "RETRY", now, PageRequest.of(0, limit))
                .stream()
                .map(PushDeliveryAttemptJpaEntity::record)
                .toList();
    }

    @Override
    public DeliveryAttemptRecord saveDeliveryAttempt(DeliveryAttemptRecord record) {
        PushDeliveryAttemptJpaEntity entity =
                deliveryAttempts
                        .findById(record.id().toString())
                        .orElseGet(PushDeliveryAttemptJpaEntity::new);
        entity.apply(record);
        return deliveryAttempts.saveAndFlush(entity).record();
    }

    private NotificationRecord record(NotificationJpaEntity entity) {
        try {
            return entity.record(json.readValue(entity.metadata(), MAP));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("알림 메타데이터를 읽을 수 없습니다.", exception);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("알림 메타데이터를 저장할 수 없습니다.", exception);
        }
    }
}
