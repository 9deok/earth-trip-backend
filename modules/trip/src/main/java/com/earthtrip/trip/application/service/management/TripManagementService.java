package com.earthtrip.trip.application.service.management;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import com.earthtrip.trip.spi.TripChangePublisher;
import com.earthtrip.trip.spi.TripMembershipLookup;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    private final TripChangePublisher changes;
    private final TripSegmentUseCase segments;

    TripManagementService(
            LoadTripPort loadPort,
            SaveTripPort savePort,
            TripMembershipLookup memberships,
            TripAccess tripAccess,
            TripChangePublisher changes,
            TripSegmentUseCase segments,
            Clock clock) {
        this.loadPort = loadPort;
        this.savePort = savePort;
        this.memberships = memberships;
        this.tripAccess = tripAccess;
        this.changes = changes;
        this.segments = segments;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResult> list(UUID actorUserId) {
        java.util.Map<UUID, Trip> accessible = new java.util.LinkedHashMap<>();
        loadPort.findAllByOwner(actorUserId)
                .forEach(trip -> accessible.put(trip.id().value(), trip));
        loadPort.findAllByIds(memberships.activeTripIds(actorUserId))
                .forEach(trip -> accessible.putIfAbsent(trip.id().value(), trip));
        return accessible.values().stream()
                .filter(trip -> trip.status() != Trip.Status.DELETION_PENDING)
                .sorted(java.util.Comparator.comparing(Trip::updatedAt).reversed())
                .map(TripManagementService::result)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResult> listDeletionPending(UUID actorUserId) {
        return loadPort.findAllByOwner(actorUserId).stream()
                .filter(trip -> trip.status() == Trip.Status.DELETION_PENDING)
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
        if (trip.status() == Trip.Status.DELETION_PENDING) {
            throw EarthTripException.conflict(
                    "TRIP_DELETION_PENDING", "삭제 대기 중인 여행은 복구한 뒤 수정할 수 있습니다.");
        }
        if ("DELETION_PENDING".equalsIgnoreCase(command.status())) {
            throw EarthTripException.badRequest(
                    "INVALID_TRIP_STATUS_TRANSITION", "여행 삭제는 삭제 전용 기능을 사용해 주세요.");
        }
        trip.update(
                command.title(),
                command.status(),
                command.startDate(),
                command.endDate(),
                command.timeZone(),
                command.defaultCurrency(),
                command.planningMode(),
                command.pace(),
                command.companionCount(),
                command.companionNames(),
                command.dateMode(),
                command.travelMode(),
                command.departurePoint(),
                command.returnPoint(),
                command.firstDayStartMinutes(),
                command.lastDayEndMinutes(),
                command.overnightTravelNights(),
                command.reduceStairs(),
                command.frequentBreaks(),
                command.walkingLimitMinutes(),
                command.dietaryNotes(),
                clock.instant());
        Trip saved = savePort.save(trip);
        changes.publish(tripId, actorUserId, "UPDATED", "TRIP", tripId);
        return result(saved);
    }

    @Override
    public TripResult requestDeletion(UUID tripId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwnerIncludingDeletionPending(tripId, actorUserId);
        Trip trip = load(tripId);
        if (trip.status() == Trip.Status.DELETION_PENDING) {
            return result(trip);
        }
        verifyVersion(trip, baseVersion);
        Instant now = clock.instant();
        trip.requestDeletion(now, now.plus(DELETION_GRACE));
        Trip saved = savePort.save(trip);
        changes.publish(tripId, actorUserId, "DELETION_REQUESTED", "TRIP", tripId);
        return result(saved);
    }

    @Override
    public TripResult restore(UUID tripId, UUID actorUserId, long baseVersion) {
        tripAccess.requireOwnerIncludingDeletionPending(tripId, actorUserId);
        Trip trip = load(tripId);
        verifyVersion(trip, baseVersion);
        try {
            trip.restoreDeletion(clock.instant());
        } catch (IllegalStateException exception) {
            throw EarthTripException.conflict("TRIP_NOT_DELETION_PENDING", exception.getMessage());
        }
        Trip saved = savePort.save(trip);
        changes.publish(tripId, actorUserId, "RESTORED", "TRIP", tripId);
        return result(saved);
    }

    @Override
    public TripResult copy(UUID tripId, UUID actorUserId, UUID requestId, String rawTitle) {
        tripAccess.requireViewer(tripId, actorUserId);
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        if (tripId.equals(requestId)) {
            throw EarthTripException.badRequest(
                    "COPY_ID_MATCHES_SOURCE", "복사본 요청 ID는 원본 여행 ID와 달라야 합니다.");
        }
        Trip source = load(tripId);
        String title =
                rawTitle == null || rawTitle.isBlank() ? source.title().value() + " 복사본" : rawTitle;
        TripId copyId = new TripId(requestId);
        Trip existing = loadPort.findById(copyId).orElse(null);
        if (existing != null) {
            if (!matchesCopyRetry(existing, source, actorUserId, title)) {
                throw EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        Trip copied =
                Trip.create(
                        copyId,
                        actorUserId,
                        new TripTitle(title),
                        source.timeZone(),
                        source.defaultCurrency(),
                        clock.instant());
        copied.update(
                null,
                null,
                source.startDate(),
                source.endDate(),
                null,
                null,
                source.planningMode().name(),
                source.pace().name(),
                source.companionCount(),
                source.companionNames(),
                source.dateMode().name(),
                source.travelMode().name(),
                source.departurePoint(),
                source.returnPoint(),
                source.firstDayStartMinutes(),
                source.lastDayEndMinutes(),
                source.overnightTravelNights(),
                source.reduceStairs(),
                source.frequentBreaks(),
                source.walkingLimitMinutes(),
                source.dietaryNotes(),
                clock.instant());
        Trip saved = savePort.save(copied);
        copySegments(tripId, saved.id().value(), actorUserId);
        changes.publish(requestId, actorUserId, "CREATED", "TRIP", requestId);
        return result(saved);
    }

    private static boolean matchesCopyRetry(
            Trip existing, Trip source, UUID actorUserId, String expectedTitle) {
        return existing.isOwnedBy(actorUserId)
                && existing.title().value().equals(expectedTitle)
                && existing.timeZone().equals(source.timeZone())
                && existing.defaultCurrency().equals(source.defaultCurrency())
                && java.util.Objects.equals(existing.startDate(), source.startDate())
                && java.util.Objects.equals(existing.endDate(), source.endDate())
                && existing.dateMode() == source.dateMode()
                && existing.travelMode() == source.travelMode();
    }

    private void copySegments(UUID sourceTripId, UUID targetTripId, UUID actorUserId) {
        for (TripSegmentUseCase.SegmentResult source : segments.list(sourceTripId, actorUserId)) {
            UUID segmentId =
                    UUID.nameUUIDFromBytes(
                            ("earthtrip:trip-copy:" + targetTripId + ":" + source.segmentId())
                                    .getBytes(StandardCharsets.UTF_8));
            segments.create(
                    targetTripId,
                    actorUserId,
                    new TripSegmentUseCase.SegmentCommand(
                            segmentId,
                            source.type(),
                            source.cityName(),
                            source.countryCode(),
                            source.placeId(),
                            source.latitude(),
                            source.longitude(),
                            source.timeZone(),
                            source.startDate(),
                            source.endDate(),
                            source.accommodationName(),
                            source.accommodationPlaceId(),
                            source.checkInAt(),
                            source.checkOutAt(),
                            source.transportMode(),
                            source.departureAt(),
                            source.arrivalAt(),
                            source.anchorAt(),
                            source.sortOrder(),
                            0));
        }
    }

    private Trip load(UUID tripId) {
        return loadPort.findById(new TripId(tripId))
                .orElseThrow(() -> EarthTripException.notFound("TRIP_NOT_FOUND", "여행을 찾을 수 없습니다."));
    }

    private static void verifyVersion(Trip trip, long baseVersion) {
        if (trip.version() != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 변경이 먼저 저장되었습니다.",
                    java.util.Map.of("serverVersion", trip.version()));
        }
    }

    private static TripResult result(Trip trip) {
        return new TripResult(
                trip.id().value(),
                trip.ownerUserId(),
                trip.title().value(),
                trip.status().name(),
                trip.startDate(),
                trip.endDate(),
                trip.timeZone(),
                trip.defaultCurrency(),
                trip.planningMode().name(),
                trip.pace().name(),
                trip.companionCount(),
                trip.companionNames(),
                trip.dateMode().name(),
                trip.travelMode().name(),
                trip.departurePoint(),
                trip.returnPoint(),
                trip.firstDayStartMinutes(),
                trip.lastDayEndMinutes(),
                trip.overnightTravelNights(),
                trip.reduceStairs(),
                trip.frequentBreaks(),
                trip.walkingLimitMinutes(),
                trip.dietaryNotes(),
                trip.version(),
                trip.createdAt(),
                trip.updatedAt(),
                trip.scheduledDeletionAt());
    }
}
