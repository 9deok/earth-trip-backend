package com.earthtrip.notification.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceStorePort {
    void saveDevice(NotificationStoreRecords.DeviceRecord record);

    Optional<NotificationStoreRecords.DeviceRecord> device(String deviceId);

    Optional<NotificationStoreRecords.DeviceRecord> deviceByTokenHash(String tokenHash);

    List<NotificationStoreRecords.DeviceRecord> activeDevices(UUID userId);
}
