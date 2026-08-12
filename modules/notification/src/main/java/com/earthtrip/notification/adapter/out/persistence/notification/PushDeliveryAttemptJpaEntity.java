package com.earthtrip.notification.adapter.out.persistence.notification;

import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_delivery_attempts")
class PushDeliveryAttemptJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "notification_id", nullable = false, length = 36)
    private String notificationId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 160)
    private String lastError;

    @Column(name = "provider_message_id", length = 300)
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushDeliveryAttemptJpaEntity() {}

    void apply(NotificationStoreRecords.DeliveryAttemptRecord record) {
        id = record.id().toString();
        notificationId = record.notificationId().toString();
        deviceId = record.deviceId();
        status = record.status();
        attempts = record.attempts();
        nextAttemptAt = record.nextAttemptAt();
        lastError = record.lastError();
        providerMessageId = record.providerMessageId();
        createdAt = record.createdAt();
        updatedAt = record.updatedAt();
    }

    NotificationStoreRecords.DeliveryAttemptRecord record() {
        return new NotificationStoreRecords.DeliveryAttemptRecord(
                UUID.fromString(id),
                UUID.fromString(notificationId),
                deviceId,
                status,
                attempts,
                nextAttemptAt,
                lastError,
                providerMessageId,
                createdAt,
                updatedAt);
    }
}
