package com.earthtrip.trip.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class DestinationCandidate {
    public enum Status { PROPOSED, SHORTLISTED, SELECTED, REJECTED }
    private final UUID id;
    private final TripId tripId;
    private String name;
    private String countryCode;
    private String placeId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String note;
    private Status status;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private final long version;

    private DestinationCandidate(
        UUID id, TripId tripId, String name, String countryCode, String placeId,
        BigDecimal latitude, BigDecimal longitude, String note, Status status,
        UUID createdBy, Instant createdAt, Instant updatedAt, long version
    ) {
        this.id = Objects.requireNonNull(id); this.tripId = Objects.requireNonNull(tripId);
        apply(name, countryCode, placeId, latitude, longitude, note, status, updatedAt);
        this.createdBy = Objects.requireNonNull(createdBy); this.createdAt = Objects.requireNonNull(createdAt);
        this.version = version;
    }
    public static DestinationCandidate create(
        UUID id, TripId tripId, String name, String countryCode, String placeId,
        BigDecimal latitude, BigDecimal longitude, String note, UUID actor, Instant now
    ) {
        return new DestinationCandidate(
            id, tripId, name, countryCode, placeId, latitude, longitude, note,
            Status.PROPOSED, actor, now, now, 0
        );
    }
    public static DestinationCandidate restore(
        UUID id, TripId tripId, String name, String countryCode, String placeId,
        BigDecimal latitude, BigDecimal longitude, String note, Status status,
        UUID createdBy, Instant createdAt, Instant updatedAt, long version
    ) {
        return new DestinationCandidate(
            id, tripId, name, countryCode, placeId, latitude, longitude, note,
            status, createdBy, createdAt, updatedAt, version
        );
    }
    public void update(
        String name, String countryCode, String placeId, BigDecimal latitude,
        BigDecimal longitude, String note, String status, Instant now
    ) {
        apply(
            name == null ? this.name : name,
            countryCode == null ? this.countryCode : countryCode,
            placeId == null ? this.placeId : placeId,
            latitude == null ? this.latitude : latitude,
            longitude == null ? this.longitude : longitude,
            note == null ? this.note : note,
            status == null ? this.status : Status.valueOf(status.strip().toUpperCase(Locale.ROOT)),
            now
        );
    }
    private void apply(
        String name, String countryCode, String placeId, BigDecimal latitude,
        BigDecimal longitude, String note, Status status, Instant now
    ) {
        if (name == null || name.isBlank() || name.strip().length() > 160) {
            throw new IllegalArgumentException("여행지 후보 이름은 1~160자여야 합니다.");
        }
        this.name = name.strip();
        this.countryCode = countryCode == null || countryCode.isBlank()
            ? null : countryCode.strip().toUpperCase(Locale.ROOT);
        this.placeId = normalize(placeId, 255); this.latitude = latitude; this.longitude = longitude;
        this.note = normalize(note, 5000); this.status = Objects.requireNonNull(status);
        this.updatedAt = Objects.requireNonNull(now);
    }
    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.strip();
        if (result.length() > max) throw new IllegalArgumentException("입력 값이 너무 깁니다.");
        return result;
    }
    public UUID id() { return id; } public TripId tripId() { return tripId; }
    public String name() { return name; } public String countryCode() { return countryCode; }
    public String placeId() { return placeId; } public BigDecimal latitude() { return latitude; }
    public BigDecimal longitude() { return longitude; } public String note() { return note; }
    public Status status() { return status; } public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
