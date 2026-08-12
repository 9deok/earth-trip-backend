package com.earthtrip.trip.application.service.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripChangePublisher;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripManagementServiceTest {

    private static final UUID ACTOR = UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void 일반_여행_목록에서_삭제_대기_여행을_제외한다() {
        Trip active = trip("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3", "나트랑 여행");
        Trip pending = trip("8ec74d24-96f4-4ec1-9125-6a8aa54c75e5", "로마 여행");
        pending.requestDeletion(NOW, NOW.plusSeconds(14 * 24 * 60 * 60));
        TripManagementService service = service(List.of(active, pending));

        var result = service.list(ACTOR);

        assertThat(result).extracting(item -> item.tripId()).containsExactly(active.id().value());
        assertThat(result).extracting(item -> item.title()).containsExactly("나트랑 여행");
    }

    @Test
    void 삭제_대기_목록은_소유자의_복구_가능한_여행만_돌려준다() {
        Trip active = trip("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3", "나트랑 여행");
        Trip pending = trip("8ec74d24-96f4-4ec1-9125-6a8aa54c75e5", "로마 여행");
        pending.requestDeletion(NOW, NOW.plusSeconds(14 * 24 * 60 * 60));
        TripManagementService service = service(List.of(active, pending));

        var result = service.listDeletionPending(ACTOR);

        assertThat(result).extracting(item -> item.tripId()).containsExactly(pending.id().value());
        assertThat(result.getFirst().scheduledDeletionAt())
                .isEqualTo(NOW.plusSeconds(14 * 24 * 60 * 60));
    }

    @Test
    void 응답을_잃은_삭제_재시도는_오래된_버전이어도_현재_삭제_예약을_반환한다() {
        Trip pending = trip("8ec74d24-96f4-4ec1-9125-6a8aa54c75e5", "로마 여행");
        pending.requestDeletion(NOW, NOW.plusSeconds(14 * 24 * 60 * 60));
        TripManagementService service = service(List.of(pending));

        var result = service.requestDeletion(pending.id().value(), ACTOR, 999);

        assertThat(result.status()).isEqualTo("DELETION_PENDING");
        assertThat(result.scheduledDeletionAt()).isEqualTo(NOW.plusSeconds(14 * 24 * 60 * 60));
    }

    private static TripManagementService service(List<Trip> trips) {
        LoadTripPort load = new InMemoryLoadPort(trips);
        SaveTripPort save = trip -> trip;
        TripMembershipLookup memberships =
                new TripMembershipLookup() {
                    @Override
                    public Optional<String> activeRole(UUID tripId, UUID userId) {
                        return Optional.empty();
                    }

                    @Override
                    public List<UUID> activeTripIds(UUID userId) {
                        return List.of();
                    }
                };
        return new TripManagementService(
                load,
                save,
                memberships,
                new UnusedTripAccess(),
                new NoOpTripChanges(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Trip trip(String id, String title) {
        return Trip.create(
                new TripId(UUID.fromString(id)),
                ACTOR,
                new TripTitle(title),
                "Asia/Seoul",
                "KRW",
                NOW);
    }

    private record InMemoryLoadPort(List<Trip> trips) implements LoadTripPort {
        @Override
        public Optional<Trip> findById(TripId tripId) {
            return trips.stream().filter(trip -> trip.id().equals(tripId)).findFirst();
        }

        @Override
        public List<Trip> findAllByOwner(UUID ownerUserId) {
            return trips;
        }

        @Override
        public List<Trip> findAllByIds(List<UUID> tripIds) {
            return List.of();
        }
    }

    private static final class UnusedTripAccess implements TripAccess {
        @Override
        public AccessResult requireViewer(UUID tripId, UUID actorUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessResult requireEditor(UUID tripId, UUID actorUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessResult requireOwner(UUID tripId, UUID actorUserId) {
            return new AccessResult(tripId, actorUserId, "OWNER", 0);
        }

        @Override
        public AccessResult requireOwnerIncludingDeletionPending(UUID tripId, UUID actorUserId) {
            return new AccessResult(tripId, actorUserId, "OWNER", 0);
        }

        @Override
        public AccessResult transferOwnership(
                UUID tripId, UUID currentOwnerUserId, UUID newOwnerUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PublicTripResult publicInfo(UUID tripId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoOpTripChanges implements TripChangePublisher {
        @Override
        public void publish(
                UUID tripId,
                UUID actorUserId,
                String action,
                String resourceType,
                UUID resourceId,
                Map<String, Object> details) {}
    }
}
