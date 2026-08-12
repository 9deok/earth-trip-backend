package com.earthtrip.notification.application.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.earthtrip.notification.application.port.out.PushTokenProtectorPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");

    @Test
    void 로그아웃으로_비활성화된_같은_기기는_새_계정으로_안전하게_이관한다() {
        TestStore store = mock(TestStore.class);
        PushTokenProtectorPort protector = mock(PushTokenProtectorPort.class);
        UUID oldUser = UUID.randomUUID();
        UUID newUser = UUID.randomUUID();
        when(store.device("device-1"))
                .thenReturn(
                        Optional.of(
                                new NotificationStoreRecords.DeviceRecord(
                                        "device-1",
                                        oldUser,
                                        "ANDROID",
                                        "old-hash",
                                        "old-cipher",
                                        1,
                                        false,
                                        NOW.minusSeconds(3600),
                                        NOW.minusSeconds(60))));
        when(protector.protect(anyString()))
                .thenReturn(new PushTokenProtectorPort.ProtectedToken("new-hash", "new-cipher"));
        NotificationService service =
                new NotificationService(
                        store, store, store, protector, Clock.fixed(NOW, ZoneOffset.UTC));

        service.registerDevice(newUser, "device-1", "android", "new-token", 2);

        ArgumentCaptor<NotificationStoreRecords.DeviceRecord> captor =
                ArgumentCaptor.forClass(NotificationStoreRecords.DeviceRecord.class);
        verify(store).saveDevice(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(newUser);
        assertThat(captor.getValue().active()).isTrue();
        assertThat(captor.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void 다른_계정에서_활성인_기기는_가로채지_않는다() {
        TestStore store = mock(TestStore.class);
        PushTokenProtectorPort protector = mock(PushTokenProtectorPort.class);
        when(store.device("device-1"))
                .thenReturn(
                        Optional.of(
                                new NotificationStoreRecords.DeviceRecord(
                                        "device-1",
                                        UUID.randomUUID(),
                                        "IOS",
                                        "hash",
                                        "cipher",
                                        1,
                                        true,
                                        NOW.minusSeconds(3600),
                                        NOW.minusSeconds(60))));
        when(protector.protect(anyString()))
                .thenReturn(new PushTokenProtectorPort.ProtectedToken("new-hash", "new-cipher"));
        NotificationService service =
                new NotificationService(
                        store, store, store, protector, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                service.registerDevice(
                                        UUID.randomUUID(), "device-1", "IOS", "new-token", 2))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        exception ->
                                assertThat(exception.code())
                                        .isEqualTo("PUSH_DEVICE_OWNED_BY_OTHER_ACCOUNT"));
    }

    @Test
    void 같은_계정의_동일한_토큰은_기존_기기_ID를_재사용한다() {
        TestStore store = mock(TestStore.class);
        PushTokenProtectorPort protector = mock(PushTokenProtectorPort.class);
        UUID user = UUID.randomUUID();
        when(store.device("new-device")).thenReturn(Optional.empty());
        when(store.deviceByTokenHash("same-hash"))
                .thenReturn(
                        Optional.of(
                                new NotificationStoreRecords.DeviceRecord(
                                        "existing-device",
                                        user,
                                        "IOS",
                                        "same-hash",
                                        "old-cipher",
                                        1,
                                        true,
                                        NOW.minusSeconds(3600),
                                        NOW.minusSeconds(60))));
        when(protector.protect(anyString()))
                .thenReturn(new PushTokenProtectorPort.ProtectedToken("same-hash", "new-cipher"));
        NotificationService service =
                new NotificationService(
                        store, store, store, protector, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.registerDevice(user, "new-device", "ios", "same-token", 2);

        ArgumentCaptor<NotificationStoreRecords.DeviceRecord> captor =
                ArgumentCaptor.forClass(NotificationStoreRecords.DeviceRecord.class);
        verify(store).saveDevice(captor.capture());
        assertThat(result.deviceId()).isEqualTo("existing-device");
        assertThat(captor.getValue().deviceId()).isEqualTo("existing-device");
        assertThat(captor.getValue().createdAt()).isEqualTo(NOW.minusSeconds(3600));
    }

    private interface TestStore
            extends NotificationRecordStorePort,
                    NotificationPreferenceStorePort,
                    PushDeviceStorePort {}
}
