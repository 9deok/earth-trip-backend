package com.earthtrip.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripTest {

    @Test
    void 여행을_생성하고_이름을_변경한다() {
        Instant createdAt = Instant.parse("2026-07-31T00:00:00Z");
        Trip trip =
                Trip.create(
                        new TripId(UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3")),
                        UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826"),
                        new TripTitle("  첫 여행  "),
                        "Asia/Seoul",
                        "KRW",
                        createdAt);

        Instant renamedAt = createdAt.plusSeconds(60);
        trip.rename(new TripTitle("부산 여행"), renamedAt);

        assertThat(trip.title().value()).isEqualTo("부산 여행");
        assertThat(trip.createdAt()).isEqualTo(createdAt);
        assertThat(trip.updatedAt()).isEqualTo(renamedAt);
    }

    @Test
    void 삭제_대기_중인_여행은_일반_수정으로_변경할_수_없다() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        Trip trip = trip(now);
        trip.requestDeletion(now, now.plusSeconds(14 * 24 * 60 * 60));

        assertThatThrownBy(
                        () ->
                                trip.update(
                                        "다시 나타난 여행",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        now.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(trip.status()).isEqualTo(Trip.Status.DELETION_PENDING);
        assertThat(trip.scheduledDeletionAt()).isNotNull();
    }

    @Test
    void 일반_수정으로_삭제_대기_상태를_만들_수_없다() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        Trip trip = trip(now);

        assertThatThrownBy(
                        () ->
                                trip.update(
                                        null,
                                        "DELETION_PENDING",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        now.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 날짜_미정으로_변경하면_확정_날짜를_제거한다() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        Trip trip = trip(now);
        trip.update(
                null,
                "PLANNING",
                LocalDate.parse("2026-10-02"),
                LocalDate.parse("2026-10-06"),
                null,
                null,
                null,
                null,
                null,
                null,
                "EXACT",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now.plusSeconds(30));

        trip.update(
                null,
                "DRAFT",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "UNDECIDED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now.plusSeconds(60));

        assertThat(trip.startDate()).isNull();
        assertThat(trip.endDate()).isNull();
        assertThat(trip.dateMode()).isEqualTo(Trip.DateMode.UNDECIDED);
        assertThat(trip.status()).isEqualTo(Trip.Status.DRAFT);
    }

    private static Trip trip(Instant now) {
        return Trip.create(
                new TripId(UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3")),
                UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826"),
                new TripTitle("로마 여행"),
                "Europe/Rome",
                "EUR",
                now);
    }
}
