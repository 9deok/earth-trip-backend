package com.earthtrip.notification.application.service.notification;

import com.earthtrip.notification.application.port.in.NotificationUseCase;
import com.earthtrip.notification.application.port.out.NotificationPreferenceStorePort;
import com.earthtrip.notification.application.port.out.NotificationRecordStorePort;
import com.earthtrip.notification.application.port.out.NotificationStoreRecords;
import com.earthtrip.notification.application.port.out.PushDeviceStorePort;
import com.earthtrip.notification.application.port.out.PushTokenProtectorPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class NotificationService implements NotificationUseCase {

    private final NotificationRecordStorePort notifications;
    private final NotificationPreferenceStorePort preferences;
    private final PushDeviceStorePort devices;
    private final PushTokenProtectorPort protector;
    private final Clock clock;

    NotificationService(
            NotificationRecordStorePort notifications,
            NotificationPreferenceStorePort preferences,
            PushDeviceStorePort devices,
            PushTokenProtectorPort protector,
            Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.devices = devices;
        this.protector = protector;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResult> list(UUID user, boolean unreadOnly) {
        return notifications.list(user).stream()
                .filter(notification -> !unreadOnly || notification.readAt() == null)
                .map(NotificationService::result)
                .toList();
    }

    @Override
    public void markRead(UUID user, UUID id, boolean read) {
        NotificationStoreRecords.NotificationRecord notification = load(user, id);
        notifications.save(
                copy(notification, read ? clock.instant() : null, notification.hiddenAt()));
    }

    @Override
    public void markManyRead(UUID user, List<UUID> ids) {
        if (ids == null || ids.size() > 200) {
            throw EarthTripException.badRequest(
                    "INVALID_NOTIFICATION_BATCH", "한 번에 최대 200개까지 처리할 수 있습니다.");
        }
        List<UUID> uniqueIds = ids.stream().distinct().toList();
        int updated = notifications.markManyRead(user, uniqueIds, clock.instant());
        if (updated != uniqueIds.size()) {
            throw EarthTripException.notFound(
                    "NOTIFICATION_NOT_FOUND", "읽음 처리할 수 없는 알림이 포함되어 있습니다.");
        }
    }

    @Override
    public void hide(UUID user, UUID id) {
        NotificationStoreRecords.NotificationRecord notification = load(user, id);
        notifications.save(copy(notification, notification.readAt(), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryResult summary(UUID user) {
        NotificationStoreRecords.NotificationSummaryRecord summary = notifications.summary(user);
        return new SummaryResult(
                summary.totalUnread(), summary.unreadByTrip(), summary.unreadByType());
    }

    @Override
    public PreferenceResult preferences(UUID user) {
        return result(loadOrCreatePreference(user));
    }

    @Override
    public PreferenceResult updatePreferences(UUID user, PreferenceCommand command) {
        NotificationStoreRecords.PreferenceRecord old = loadOrCreatePreference(user);
        LocalTime start = command.quietStart() == null ? old.quietStart() : command.quietStart();
        LocalTime end = command.quietEnd() == null ? old.quietEnd() : command.quietEnd();
        String zone =
                command.quietTimeZone() == null
                        ? old.quietTimeZone()
                        : ZoneId.of(command.quietTimeZone()).getId();
        if ((start == null) != (end == null)) {
            throw EarthTripException.badRequest(
                    "INVALID_QUIET_HOURS", "조용한 시간 시작과 종료를 함께 입력해 주세요.");
        }
        return result(
                preferences.savePreference(
                        new NotificationStoreRecords.PreferenceRecord(
                                user,
                                value(command.mentionsEnabled(), old.mentions()),
                                value(command.scheduleEnabled(), old.schedule()),
                                value(command.expenseEnabled(), old.expense()),
                                value(command.invitationEnabled(), old.invitation()),
                                value(command.pushEnabled(), old.push()),
                                value(command.emailEnabled(), old.email()),
                                start,
                                end,
                                zone,
                                old.version(),
                                clock.instant())));
    }

    @Override
    public DeviceResult registerDevice(
            UUID user, String deviceId, String platform, String token, int appBuild) {
        validateDevice(deviceId, appBuild);
        String normalizedPlatform = normalizePlatform(platform);
        PushTokenProtectorPort.ProtectedToken protectedToken = protector.protect(token);
        NotificationStoreRecords.DeviceRecord requestedDevice =
                devices.device(deviceId).orElse(null);
        rejectActiveOtherAccount(requestedDevice, user);

        NotificationStoreRecords.DeviceRecord tokenDevice =
                devices.deviceByTokenHash(protectedToken.hash()).orElse(null);
        rejectActiveOtherAccount(tokenDevice, user);

        NotificationStoreRecords.DeviceRecord canonical =
                tokenDevice == null ? requestedDevice : tokenDevice;
        String canonicalDeviceId = canonical == null ? deviceId : canonical.deviceId();
        Instant now = clock.instant();
        Instant createdAt =
                canonical == null || !canonical.userId().equals(user) ? now : canonical.createdAt();
        devices.saveDevice(
                new NotificationStoreRecords.DeviceRecord(
                        canonicalDeviceId,
                        user,
                        normalizedPlatform,
                        protectedToken.hash(),
                        protectedToken.cipher(),
                        appBuild,
                        true,
                        createdAt,
                        now));
        return new DeviceResult(canonicalDeviceId);
    }

    @Override
    public void removeDevice(UUID user, String deviceId) {
        NotificationStoreRecords.DeviceRecord old =
                devices.device(deviceId)
                        .filter(device -> device.userId().equals(user))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "PUSH_DEVICE_NOT_FOUND", "푸시 기기를 찾을 수 없습니다."));
        devices.saveDevice(
                new NotificationStoreRecords.DeviceRecord(
                        old.deviceId(),
                        old.userId(),
                        old.platform(),
                        old.tokenHash(),
                        old.tokenCipher(),
                        old.appBuild(),
                        false,
                        old.createdAt(),
                        clock.instant()));
    }

    private void validateDevice(String deviceId, int appBuild) {
        if (deviceId == null || deviceId.isBlank() || deviceId.length() > 100 || appBuild < 1) {
            throw EarthTripException.badRequest("INVALID_PUSH_DEVICE", "푸시 기기 정보를 확인해 주세요.");
        }
    }

    private String normalizePlatform(String platform) {
        String normalized = platform.strip().toUpperCase(Locale.ROOT);
        if (!Set.of("IOS", "ANDROID").contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_PUSH_PLATFORM", "플랫폼은 IOS 또는 ANDROID여야 합니다.");
        }
        return normalized;
    }

    private void rejectActiveOtherAccount(NotificationStoreRecords.DeviceRecord device, UUID user) {
        if (device != null && device.active() && !device.userId().equals(user)) {
            throw EarthTripException.conflict(
                    "PUSH_DEVICE_OWNED_BY_OTHER_ACCOUNT", "다른 로그인 계정에서 사용 중인 기기입니다.");
        }
    }

    private NotificationStoreRecords.NotificationRecord load(UUID user, UUID id) {
        return notifications
                .find(id)
                .filter(notification -> notification.userId().equals(user))
                .filter(notification -> notification.hiddenAt() == null)
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."));
    }

    private NotificationStoreRecords.PreferenceRecord loadOrCreatePreference(UUID user) {
        return preferences
                .preference(user)
                .orElseGet(
                        () ->
                                preferences.savePreference(
                                        new NotificationStoreRecords.PreferenceRecord(
                                                user,
                                                true,
                                                true,
                                                true,
                                                true,
                                                true,
                                                false,
                                                null,
                                                null,
                                                "Asia/Seoul",
                                                0,
                                                clock.instant())));
    }

    private static boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private static NotificationStoreRecords.NotificationRecord copy(
            NotificationStoreRecords.NotificationRecord notification,
            Instant readAt,
            Instant hiddenAt) {
        return new NotificationStoreRecords.NotificationRecord(
                notification.id(),
                notification.userId(),
                notification.tripId(),
                notification.type(),
                notification.title(),
                notification.body(),
                notification.deepLink(),
                notification.metadata(),
                notification.createdAt(),
                readAt,
                hiddenAt,
                notification.version());
    }

    private static NotificationResult result(
            NotificationStoreRecords.NotificationRecord notification) {
        return new NotificationResult(
                notification.id(),
                notification.tripId(),
                notification.type(),
                notification.title(),
                notification.body(),
                notification.deepLink(),
                notification.metadata(),
                notification.createdAt(),
                notification.readAt(),
                notification.version());
    }

    private static PreferenceResult result(NotificationStoreRecords.PreferenceRecord preference) {
        return new PreferenceResult(
                preference.mentions(),
                preference.schedule(),
                preference.expense(),
                preference.invitation(),
                preference.push(),
                preference.email(),
                preference.quietStart(),
                preference.quietEnd(),
                preference.quietTimeZone(),
                preference.version(),
                preference.updatedAt());
    }
}
