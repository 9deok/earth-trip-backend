package com.earthtrip.notification.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Objects;

public final class NotificationDeliveryPolicy {

    private NotificationDeliveryPolicy() {}

    public static boolean allowsPush(String notificationType, Preference preference, Instant now) {
        if (preference == null) {
            return true;
        }
        if (!preference.push() || !categoryEnabled(normalized(notificationType), preference)) {
            return false;
        }
        return !inQuietHours(preference, Objects.requireNonNull(now));
    }

    private static boolean categoryEnabled(String type, Preference preference) {
        if (type.contains("MENTION")) {
            return preference.mentions();
        }
        if (type.contains("SCHEDULE") || type.contains("CALENDAR")) {
            return preference.schedule();
        }
        if (type.contains("EXPENSE") || type.contains("WALLET")) {
            return preference.expense();
        }
        if (type.contains("INVITATION") || type.contains("INVITE")) {
            return preference.invitation();
        }
        return true;
    }

    private static boolean inQuietHours(Preference preference, Instant now) {
        if (preference.quietStart() == null || preference.quietEnd() == null) {
            return false;
        }
        LocalTime localNow =
                ZonedDateTime.ofInstant(now, ZoneId.of(preference.quietTimeZone())).toLocalTime();
        LocalTime start = preference.quietStart();
        LocalTime end = preference.quietEnd();
        if (start.equals(end)) {
            return true;
        }
        return start.isBefore(end)
                ? !localNow.isBefore(start) && localNow.isBefore(end)
                : !localNow.isBefore(start) || localNow.isBefore(end);
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("알림 유형은 필수입니다.");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }

    public record Preference(
            boolean mentions,
            boolean schedule,
            boolean expense,
            boolean invitation,
            boolean push,
            LocalTime quietStart,
            LocalTime quietEnd,
            String quietTimeZone) {}
}
