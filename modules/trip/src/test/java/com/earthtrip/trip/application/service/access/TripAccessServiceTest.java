package com.earthtrip.trip.application.service.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import com.earthtrip.trip.spi.TripMembershipLookup;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripAccessServiceTest {

    private static final UUID OWNER = UUID.fromString("fb3f523b-86aa-4302-95e6-8cfeba30742d");
    private static final UUID MEMBER = UUID.fromString("89fb9897-36c7-49c2-af9b-ec27485f67cf");
    private static final UUID TRIP_ID = UUID.fromString("515fb4d1-04fb-4ef2-9568-3927a55c0d63");
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 삭제_대기_여행은_직접_조회하거나_일반_권한으로_수정할_수_없다() {
        Trip trip =
                Trip.create(
                        new TripId(TRIP_ID),
                        OWNER,
                        new TripTitle("삭제 대기 여행"),
                        "Asia/Seoul",
                        "KRW",
                        NOW);
        trip.requestDeletion(NOW, NOW.plusSeconds(14 * 24 * 60 * 60));
        TripAccessService service = service(trip);

        assertUnavailable(() -> service.requireViewer(TRIP_ID, MEMBER));
        assertUnavailable(() -> service.requireEditor(TRIP_ID, MEMBER));
        assertUnavailable(() -> service.requireOwner(TRIP_ID, OWNER));
        assertUnavailable(() -> service.publicInfo(TRIP_ID));
        assertThat(service.requireOwnerIncludingDeletionPending(TRIP_ID, OWNER).role())
                .isEqualTo("OWNER");
    }

    private static TripAccessService service(Trip trip) {
        LoadTripPort trips =
                new LoadTripPort() {
                    @Override
                    public Optional<Trip> findById(TripId tripId) {
                        return trip.id().equals(tripId) ? Optional.of(trip) : Optional.empty();
                    }

                    @Override
                    public List<Trip> findAllByOwner(UUID ownerUserId) {
                        return List.of();
                    }

                    @Override
                    public List<Trip> findAllByIds(List<UUID> tripIds) {
                        return List.of();
                    }
                };
        SaveTripPort saves = saved -> saved;
        TripMembershipLookup memberships =
                new TripMembershipLookup() {
                    @Override
                    public Optional<String> activeRole(UUID tripId, UUID userId) {
                        return userId.equals(MEMBER) ? Optional.of("EDITOR") : Optional.empty();
                    }

                    @Override
                    public List<UUID> activeTripIds(UUID userId) {
                        return List.of();
                    }
                };
        return new TripAccessService(trips, saves, memberships, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static void assertUnavailable(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> {
                            assertThat(error.code()).isEqualTo("TRIP_NOT_FOUND");
                            assertThat(error.httpStatus()).isEqualTo(404);
                        });
    }
}
