package com.earthtrip.trip.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class TripPolicy {
    private TripPolicy() {}

    static void dates(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("출발일과 종료일은 함께 입력해야 합니다.");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("여행 종료일은 출발일보다 빠를 수 없습니다.");
        }
        if (startDate != null && ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw new IllegalArgumentException("한 여행은 최대 366일까지 설정할 수 있습니다.");
        }
    }

    static String timeZone(String value) {
        return ZoneId.of(Objects.requireNonNull(value).strip()).getId();
    }

    static String currency(String value) {
        return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
    }

    static Trip.Status updatedStatus(Trip.Status current, String rawStatus) {
        if (current == Trip.Status.DELETION_PENDING) {
            throw new IllegalStateException("삭제 대기 중인 여행은 복구한 뒤 수정할 수 있습니다.");
        }
        Trip.Status candidate =
                rawStatus == null
                        ? current
                        : Trip.Status.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        if (candidate == Trip.Status.DELETION_PENDING) {
            throw new IllegalArgumentException("여행 삭제는 삭제 전용 기능을 사용해야 합니다.");
        }
        return candidate;
    }

    static Preferences preferences(
            int companionCount,
            List<String> companionNames,
            Trip.DateMode dateMode,
            Trip.TravelMode travelMode,
            String departurePoint,
            String returnPoint,
            int firstDayStartMinutes,
            int lastDayEndMinutes,
            int overnightTravelNights,
            boolean reduceStairs,
            boolean frequentBreaks,
            int walkingLimitMinutes,
            String dietaryNotes) {
        if (companionCount < 1 || companionCount > 20) {
            throw new IllegalArgumentException("동행자 수는 1명에서 20명 사이여야 합니다.");
        }
        List<String> names =
                (companionNames == null ? List.<String>of() : companionNames)
                        .stream()
                                .map(value -> text(value, 80))
                                .filter(value -> !value.isEmpty())
                                .distinct()
                                .toList();
        if (names.size() > companionCount) {
            throw new IllegalArgumentException("동행자 이름 수가 동행자 수보다 많습니다.");
        }
        minute(firstDayStartMinutes);
        minute(lastDayEndMinutes);
        if (overnightTravelNights < 0 || overnightTravelNights > 30) {
            throw new IllegalArgumentException("야간 이동 숙박 수를 확인해 주세요.");
        }
        if (walkingLimitMinutes < 15 || walkingLimitMinutes > 480) {
            throw new IllegalArgumentException("도보 한도는 15분에서 480분 사이여야 합니다.");
        }
        return new Preferences(
                companionCount,
                List.copyOf(names),
                Objects.requireNonNull(dateMode),
                Objects.requireNonNull(travelMode),
                text(departurePoint, 200),
                text(returnPoint, 200),
                firstDayStartMinutes,
                lastDayEndMinutes,
                overnightTravelNights,
                reduceStairs,
                frequentBreaks,
                walkingLimitMinutes,
                text(dietaryNotes, 2_000));
    }

    private static void minute(int value) {
        if (value < 0 || value > 1_439) {
            throw new IllegalArgumentException("하루 시각은 0분에서 1439분 사이여야 합니다.");
        }
    }

    private static String text(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("입력 값이 너무 깁니다.");
        }
        return normalized;
    }

    record Preferences(
            int companionCount,
            List<String> companionNames,
            Trip.DateMode dateMode,
            Trip.TravelMode travelMode,
            String departurePoint,
            String returnPoint,
            int firstDayStartMinutes,
            int lastDayEndMinutes,
            int overnightTravelNights,
            boolean reduceStairs,
            boolean frequentBreaks,
            int walkingLimitMinutes,
            String dietaryNotes) {}
}
