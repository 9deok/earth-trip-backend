package com.earthtrip.notification.application.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.notification.application.port.in.NotificationPublishUseCase;
import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationPublisherServiceTest {

    @Test
    void 입력_포트에서_알림을_저장하고_전달_결과를_반환한다() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-24T01:00:00Z");
        NotificationRecordStorePort notifications = mock(NotificationRecordStorePort.class);
        NotificationPreferenceStorePort preferences = mock(NotificationPreferenceStorePort.class);
        PushDeviceStorePort devices = mock(PushDeviceStorePort.class);
        PushDeliveryCoordinator deliveries = mock(PushDeliveryCoordinator.class);
        when(notifications.find(notificationId)).thenReturn(Optional.empty());
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(preferences.preference(userId)).thenReturn(Optional.empty());
        when(devices.activeDevices(userId)).thenReturn(java.util.List.of());
        when(deliveries.deliver(any(), anyList()))
                .thenReturn(new PushDeliveryCoordinator.DeliverySummary(2, 1));
        NotificationPublisherService service =
                new NotificationPublisherService(
                        notifications,
                        preferences,
                        devices,
                        deliveries,
                        Clock.fixed(now, ZoneOffset.UTC));

        NotificationPublishUseCase.PublishNotificationResult result =
                service.publish(
                        new NotificationPublishUseCase.PublishNotificationCommand(
                                notificationId,
                                userId,
                                tripId,
                                "schedule_changed",
                                "일정 변경",
                                "일정이 변경되었습니다.",
                                "earthtrip://schedule",
                                Map.of("source", "admin")));

        assertThat(result.notificationId()).isEqualTo(notificationId);
        assertThat(result.deliveredDevices()).isEqualTo(2);
        assertThat(result.failedDevices()).isEqualTo(1);
        assertThat(result.skippedDevices()).isZero();
        ArgumentCaptor<NotificationStoreRecords.NotificationRecord> saved =
                ArgumentCaptor.forClass(NotificationStoreRecords.NotificationRecord.class);
        org.mockito.Mockito.verify(notifications).save(saved.capture());
        assertThat(saved.getValue().type()).isEqualTo("SCHEDULE_CHANGED");
        assertThat(saved.getValue().createdAt()).isEqualTo(now);
        assertThat(saved.getValue().metadata()).containsEntry("source", "admin");
    }
}
