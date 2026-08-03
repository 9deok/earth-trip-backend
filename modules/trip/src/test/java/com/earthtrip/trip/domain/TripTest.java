package com.earthtrip.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripTest {

    @Test
    void 여행을_생성하고_이름을_변경한다() {
        Instant createdAt = Instant.parse("2026-07-31T00:00:00Z");
        Trip trip = Trip.create(
            new TripId(UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3")),
            new TripTitle("  첫 여행  "),
            createdAt
        );

        Instant renamedAt = createdAt.plusSeconds(60);
        trip.rename(new TripTitle("부산 여행"), renamedAt);

        assertThat(trip.title().value()).isEqualTo("부산 여행");
        assertThat(trip.createdAt()).isEqualTo(createdAt);
        assertThat(trip.updatedAt()).isEqualTo(renamedAt);
    }
}
