package com.earthtrip.trip.domain;

import java.util.Objects;
import java.util.UUID;

public record TripId(UUID value) {

    public TripId {
        Objects.requireNonNull(value, "여행 ID는 필수입니다.");
    }

    public static TripId from(String value) {
        return new TripId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
