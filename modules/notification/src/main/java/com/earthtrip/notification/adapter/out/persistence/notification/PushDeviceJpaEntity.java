package com.earthtrip.notification.adapter.out.persistence.notification;

import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_devices")
class PushDeviceJpaEntity {
    @Id
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_cipher", nullable = false, columnDefinition = "TEXT")
    private String tokenCipher;

    @Column(name = "app_build", nullable = false)
    private int appBuild;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushDeviceJpaEntity() {}

    void apply(NotificationStoreRecords.DeviceRecord r) {
        deviceId = r.deviceId();
        userId = r.userId().toString();
        platform = r.platform();
        tokenHash = r.tokenHash();
        tokenCipher = r.tokenCipher();
        appBuild = r.appBuild();
        active = r.active();
        createdAt = r.createdAt();
        updatedAt = r.updatedAt();
    }

    NotificationStoreRecords.DeviceRecord record() {
        return new NotificationStoreRecords.DeviceRecord(
                deviceId,
                UUID.fromString(userId),
                platform,
                tokenHash,
                tokenCipher,
                appBuild,
                active,
                createdAt,
                updatedAt);
    }
}
