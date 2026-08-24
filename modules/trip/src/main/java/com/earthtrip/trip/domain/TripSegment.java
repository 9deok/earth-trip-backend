package com.earthtrip.trip.domain;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class TripSegment {

    public enum Type {
        STAY,
        TRANSFER,
        OVERNIGHT_TRANSFER
    }

    private final UUID id;
    private final TripId tripId;
    private Type type;
    private String cityName;
    private String countryCode;
    private String placeId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timeZone;
    private LocalDate startDate;
    private LocalDate endDate;
    private String accommodationName;
    private String accommodationPlaceId;
    private Instant checkInAt;
    private Instant checkOutAt;
    private String transportMode;
    private Instant departureAt;
    private Instant arrivalAt;
    private Instant anchorAt;
    private int sortOrder;
    private final UUID createdBy;
    private UUID updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private final long version;

    private TripSegment(
            UUID id,
            TripId tripId,
            Type type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.tripId = Objects.requireNonNull(tripId);
        apply(
                type,
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                timeZone,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                anchorAt,
                sortOrder,
                updatedBy,
                updatedAt);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.version = version;
    }

    public static TripSegment create(
            UUID id,
            TripId tripId,
            Type type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            UUID actorUserId,
            Instant now) {
        return new TripSegment(
                id,
                tripId,
                type,
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                timeZone,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                anchorAt,
                sortOrder,
                actorUserId,
                actorUserId,
                now,
                now,
                0);
    }

    public static TripSegment restore(
            UUID id,
            TripId tripId,
            Type type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new TripSegment(
                id,
                tripId,
                type,
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                timeZone,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                anchorAt,
                sortOrder,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt,
                version);
    }

    public void update(
            Type type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            UUID actorUserId,
            Instant now) {
        apply(
                type,
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                timeZone,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                anchorAt,
                sortOrder,
                actorUserId,
                now);
    }

    public void moveTo(int newSortOrder, UUID actorUserId, Instant now) {
        if (newSortOrder < 0) throw new IllegalArgumentException("정렬 순서는 0 이상이어야 합니다.");
        sortOrder = newSortOrder;
        updatedBy = Objects.requireNonNull(actorUserId);
        updatedAt = Objects.requireNonNull(now);
    }

    private void apply(
            Type type,
            String cityName,
            String countryCode,
            String placeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String timeZone,
            LocalDate startDate,
            LocalDate endDate,
            String accommodationName,
            String accommodationPlaceId,
            Instant checkInAt,
            Instant checkOutAt,
            String transportMode,
            Instant departureAt,
            Instant arrivalAt,
            Instant anchorAt,
            int sortOrder,
            UUID actorUserId,
            Instant now) {
        this.type = Objects.requireNonNull(type);
        if (type == Type.STAY && (cityName == null || cityName.isBlank())) {
            throw new IllegalArgumentException("체류 구간에는 도시 이름이 필요합니다.");
        }
        if ((startDate == null) != (endDate == null)
                || (startDate != null && endDate.isBefore(startDate))) {
            throw new IllegalArgumentException("구간 시작일과 종료일을 확인해 주세요.");
        }
        if (checkInAt != null && checkOutAt != null && checkOutAt.isBefore(checkInAt)) {
            throw new IllegalArgumentException("체크아웃 시각은 체크인보다 빠를 수 없습니다.");
        }
        if (departureAt != null && arrivalAt != null && arrivalAt.isBefore(departureAt)) {
            throw new IllegalArgumentException("도착 시각은 출발 시각보다 빠를 수 없습니다.");
        }
        if (sortOrder < 0) throw new IllegalArgumentException("정렬 순서는 0 이상이어야 합니다.");
        this.cityName = normalize(cityName, 160);
        this.countryCode = normalizeCountryCode(countryCode);
        this.placeId = normalize(placeId, 255);
        validateCoordinates(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = normalizeTimeZone(timeZone);
        this.startDate = startDate;
        this.endDate = endDate;
        this.accommodationName = normalize(accommodationName, 200);
        this.accommodationPlaceId = normalize(accommodationPlaceId, 255);
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.transportMode = normalize(transportMode, 40);
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.anchorAt = anchorAt;
        this.sortOrder = sortOrder;
        this.updatedBy = Objects.requireNonNull(actorUserId);
        this.updatedAt = Objects.requireNonNull(now);
    }

    private static String normalize(String value, int max) {
        if (value == null) return null;
        String normalized = value.strip();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > max) throw new IllegalArgumentException("입력 값이 너무 깁니다.");
        return normalized;
    }

    private static String normalizeCountryCode(String value) {
        String normalized = normalize(value, 2);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("국가 코드는 영문 두 글자여야 합니다.");
        }
        return normalized;
    }

    private static String normalizeTimeZone(String value) {
        String normalized = normalize(value, 80);
        if (normalized == null) return null;
        try {
            ZoneId.of(normalized);
            return normalized;
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("올바른 시간대를 입력해 주세요.");
        }
    }

    private static void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null
                && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                        || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException("위도는 -90에서 90 사이여야 합니다.");
        }
        if (longitude != null
                && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                        || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException("경도는 -180에서 180 사이여야 합니다.");
        }
    }

    public UUID id() {
        return id;
    }

    public TripId tripId() {
        return tripId;
    }

    public Type type() {
        return type;
    }

    public String cityName() {
        return cityName;
    }

    public String countryCode() {
        return countryCode;
    }

    public String placeId() {
        return placeId;
    }

    public BigDecimal latitude() {
        return latitude;
    }

    public BigDecimal longitude() {
        return longitude;
    }

    public String timeZone() {
        return timeZone;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public String accommodationName() {
        return accommodationName;
    }

    public String accommodationPlaceId() {
        return accommodationPlaceId;
    }

    public Instant checkInAt() {
        return checkInAt;
    }

    public Instant checkOutAt() {
        return checkOutAt;
    }

    public String transportMode() {
        return transportMode;
    }

    public Instant departureAt() {
        return departureAt;
    }

    public Instant arrivalAt() {
        return arrivalAt;
    }

    public Instant anchorAt() {
        return anchorAt;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
