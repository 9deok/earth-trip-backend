package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.api.PushDeliveryEvents;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PushDeliveryEventService implements PushDeliveryEvents {

    private static final Set<String> STATUSES =
            Set.of("DELIVERED", "TEMPORARY_FAILURE", "INVALID_TOKEN", "UNREGISTERED");

    private final PushDeviceStorePort devices;
    private final Clock clock;

    PushDeliveryEventService(PushDeviceStorePort devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    @Override
    public void recordDelivery(String deviceId, String status, String providerMessageId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw EarthTripException.badRequest(
                    "PUSH_DEVICE_ID_REQUIRED", "푸시 제공자 응답에 deviceId가 필요합니다.");
        }
        String normalizedStatus = status == null ? "" : status.strip().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalizedStatus)) {
            throw EarthTripException.badRequest(
                    "INVALID_PUSH_DELIVERY_STATUS", "지원하지 않는 푸시 전송 상태입니다.");
        }
        NotificationStoreRecords.DeviceRecord device =
                devices.device(deviceId)
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "PUSH_DEVICE_NOT_FOUND", "푸시 기기를 찾을 수 없습니다."));
        if (normalizedStatus.equals("INVALID_TOKEN") || normalizedStatus.equals("UNREGISTERED")) {
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
                            clock.instant()));
        }
    }
}
