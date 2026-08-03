package com.earthtrip.trip.domain;

import java.util.Objects;

public record TripTitle(String value) {

    private static final int MAX_LENGTH = 100;

    public TripTitle {
        Objects.requireNonNull(value, "여행 이름은 필수입니다.");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("여행 이름은 비어 있을 수 없습니다.");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("여행 이름은 100자를 넘을 수 없습니다.");
        }
        value = normalized;
    }
}
