package com.earthtrip.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripSegmentTest {

    private static final UUID ACTOR = UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void 날짜가_없어도_선택한_도시_구간을_만들_수_있다() {
        TripSegment segment = segment(null, null);

        assertThat(segment.cityName()).isEqualTo("도쿄");
        assertThat(segment.startDate()).isNull();
        assertThat(segment.endDate()).isNull();
    }

    @Test
    void 구간_날짜는_둘_다_있거나_둘_다_없어야_한다() {
        assertThatThrownBy(() -> segment(LocalDate.parse("2026-10-02"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TripSegment segment(LocalDate startDate, LocalDate endDate) {
        return TripSegment.create(
                UUID.fromString("4d2dbb19-4ea8-4c7b-ae43-b7db2c34788a"),
                new TripId(UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3")),
                TripSegment.Type.STAY,
                "도쿄",
                "JP",
                "ChIJ51cu8IcbXWARiRtXIothAS4",
                null,
                null,
                startDate,
                endDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                ACTOR,
                NOW);
    }
}
