package com.earthtrip.trip.application.service.management;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.earthtrip.trip.spi.TripMembershipLookup;
import com.earthtrip.trip.api.TripAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripManagementService implements TripManagementUseCase {

    private static final Duration DELETION_GRACE = Duration.ofDays(14);
    private final LoadTripPort loadPort;
    private final SaveTripPort savePort;
    private final Clock clock;
    private final TripMembershipLookup memberships;
    private final TripAccess tripAccess;

    TripManagementService(
        LoadTripPort loadPort,
        SaveTripPort savePort,
        TripMembershipLookup memberships,
        TripAccess tripAccess,
        Clock clock
    ) {
        this.loadPort = loadPort;
        this.savePort = savePort;
        this.memberships = memberships;
        this.tripAccess = tripAccess;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResult> list(UUID actorUserId) {
        java.util.Map<UUID, Trip> accessible = new java.util.LinkedHashMap<>();
        loadPort.findAllByOwner(actorUserId).forEach(trip -> accessible.put(trip.id().value(), trip));
        loadPort.findAllByIds(memberships.activeTripIds(actorUserId))
            .forEach(trip -> accessible.putIfAbsent(trip.id().value(), trip));
        return accessible.values().stream()
            .sorted(java.util.Comparator.comparing(Trip::updatedAt).reversed())
            .map(TripManagementService::result)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResult get(UUID tripId, UUID actorUserId) {
        tripAccess.requireViewer(tripId, actorUserId);
        return result(load(tripId));
    }

    @Override
    public TripResult update(UUID tripId, UUID actorUserId, UpdateTripCommand command) {
        tripAccess.requireEditor(tripId, actorUserId);
        Trip trip = load(tripId);
        verifyVersion(trip, command.baseVersion());
        trip.update(
            command.title(), command.status(), command.startDate(), command.endDate(),
            command.timeZone(), command.defaultCurrency(), command.planningMode(), command.pace(),
            clock.instant()
        );
        return result(savePort.save(trip));
    }

    @Override
    public TripResult requestDeletion(UUID tripId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        Trip trip = load(tripId);
        verifyVersion(trip, baseVersion);
        Instant now = clock.instant();
        trip.requestDeletion(now, now.plus(DELETION_GRACE));
        return result(savePort.save(trip));
    }

    @Override
    public TripResult restore(UUID tripId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwner(tripId, actorUserId);
        Trip trip = load(tripId);
        verifyVersion(trip, baseVersion);
        try {
            trip.restoreDeletion(clock.instant());
        } catch (IllegalStateException exception) {
            throw EarthTripException.conflict("TRIP_NOT_DELETION_PENDING", exception.getMessage());
        }
        return result(savePort.save(trip));
    }

    @Override
    public TripResult copy(UUID tripId, UUID actorUserId, UUID requestId, String rawTitle) {
        tripAccess.requireViewer(tripId, actorUserId);
        Trip source = load(tripId);
        TripId copyId = new TripId(requestId);
        Trip existing = loadPort.findById(copyId).orElse(null);
        if (existing != null) {
            if (!existing.isOwnedBy(actorUserId)) {
                throw EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        String title = rawTitle == null || rawTitle.isBlank()
            ? source.title().value() + " 복사본"
            : rawTitle;
        Trip copied = Trip.create(
            copyId, actorUserId, new TripTitle(title), source.timeZone(), source.defaultCurrency(), clock.instant()
        );
        copied.update(
            null, null, source.startDate(), source.endDate(), null, null,
            source.planningMode().name(), source.pace().name(), clock.instant()
        );
        return result(savePort.save(copied));
    }

    private Trip load(UUID tripId) {
        return loadPort.findById(new TripId(tripId))
            .orElseThrow(() -> EarthTripException.notFound("TRIP_NOT_FOUND", "여행을 찾을 수 없습니다."));
    }

    private static void verifyVersion(Trip trip, long baseVersion) {
        if (trip.version() != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 변경이 먼저 저장되었습니다.",
                java.util.Map.of("serverVersion", trip.version())
            );
        }
    }

    private static TripResult result(Trip trip) {
        return new TripResult(
            trip.id().value(), trip.ownerUserId(), trip.title().value(), trip.status().name(),
            trip.startDate(), trip.endDate(), trip.timeZone(), trip.defaultCurrency(),
            trip.planningMode().name(), trip.pace().name(), trip.version(), trip.createdAt(),
            trip.updatedAt(), trip.scheduledDeletionAt()
        );
    }
}
