package com.earthtrip.trip.application.service.access;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.spi.TripMembershipLookup;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripAccessService implements TripAccess {
    private final LoadTripPort trips;
    private final TripMembershipLookup memberships;
    private final SaveTripPort saveTripPort;
    private final Clock clock;

    TripAccessService(
            LoadTripPort trips,
            SaveTripPort saveTripPort,
            TripMembershipLookup memberships,
            Clock clock) {
        this.trips = trips;
        this.saveTripPort = saveTripPort;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public AccessResult requireViewer(UUID tripId, UUID actorUserId) {
        return require(tripId, actorUserId, "VIEWER");
    }

    @Override
    public AccessResult requireEditor(UUID tripId, UUID actorUserId) {
        return require(tripId, actorUserId, "EDITOR");
    }

    @Override
    public AccessResult requireOwner(UUID tripId, UUID actorUserId) {
        return require(tripId, actorUserId, "OWNER");
    }

    @Override
    public AccessResult requireOwnerIncludingDeletionPending(UUID tripId, UUID actorUserId) {
        Trip trip = load(tripId);
        if (!trip.isOwnedBy(actorUserId)) {
            throw tripNotFound();
        }
        return new AccessResult(tripId, trip.ownerUserId(), "OWNER", trip.version());
    }

    @Override
    @Transactional
    public AccessResult transferOwnership(
            UUID tripId, UUID currentOwnerUserId, UUID newOwnerUserId) {
        Trip trip = loadAvailable(tripId);
        if (!trip.isOwnedBy(currentOwnerUserId)) {
            throw tripNotFound();
        }
        try {
            trip.transferOwnership(newOwnerUserId, clock.instant());
        } catch (IllegalStateException exception) {
            throw EarthTripException.conflict(
                    "OWNERSHIP_TRANSFER_NOT_ALLOWED", exception.getMessage());
        }
        Trip saved = saveTripPort.save(trip);
        return new AccessResult(saved.id().value(), saved.ownerUserId(), "OWNER", saved.version());
    }

    @Override
    public PublicTripResult publicInfo(UUID tripId) {
        Trip trip = loadAvailable(tripId);
        return new PublicTripResult(
                trip.id().value(),
                trip.title().value(),
                trip.startDate(),
                trip.endDate(),
                trip.timeZone(),
                trip.status().name());
    }

    private AccessResult require(UUID tripId, UUID actorUserId, String minimumRole) {
        Trip trip = loadAvailable(tripId);
        String role =
                trip.isOwnedBy(actorUserId)
                        ? "OWNER"
                        : memberships.activeRole(tripId, actorUserId).orElse(null);
        if (role == null || rank(role) < rank(minimumRole)) {
            throw EarthTripException.notFound("TRIP_NOT_FOUND", "여행을 찾을 수 없습니다.");
        }
        return new AccessResult(tripId, trip.ownerUserId(), role, trip.version());
    }

    private Trip loadAvailable(UUID tripId) {
        Trip trip = load(tripId);
        if (trip.status() == Trip.Status.DELETION_PENDING) {
            throw tripNotFound();
        }
        return trip;
    }

    private Trip load(UUID tripId) {
        return trips.findById(new TripId(tripId)).orElseThrow(TripAccessService::tripNotFound);
    }

    private static EarthTripException tripNotFound() {
        return EarthTripException.notFound("TRIP_NOT_FOUND", "여행을 찾을 수 없습니다.");
    }

    private static int rank(String role) {
        return switch (role) {
            case "OWNER" -> 3;
            case "EDITOR" -> 2;
            case "VIEWER" -> 1;
            default -> 0;
        };
    }
}
