package com.earthtrip.trip.application.service.segment;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripChangePublisher;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.out.TripSegmentStorePort;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripSegment;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripSegmentService implements TripSegmentUseCase {
    private final TripAccess tripAccess;
    private final TripSegmentStorePort segmentStore;
    private final TripChangePublisher changes;
    private final Clock clock;

    TripSegmentService(
            TripAccess tripAccess,
            TripSegmentStorePort segmentStore,
            TripChangePublisher changes,
            Clock clock) {
        this.tripAccess = tripAccess;
        this.segmentStore = segmentStore;
        this.changes = changes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentResult> list(UUID tripId, UUID actorUserId) {
        tripAccess.requireViewer(tripId, actorUserId);
        return segmentStore.findAll(new TripId(tripId)).stream()
                .map(TripSegmentService::result)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SegmentResult get(UUID tripId, UUID segmentId, UUID actorUserId) {
        tripAccess.requireViewer(tripId, actorUserId);
        return result(loadInTrip(tripId, segmentId));
    }

    @Override
    public SegmentResult create(UUID tripId, UUID actorUserId, SegmentCommand command) {
        tripAccess.requireEditor(tripId, actorUserId);
        TripSegment existing = segmentStore.findById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().value().equals(tripId)) throw idempotencyConflict();
            return result(existing);
        }
        int sortOrder =
                command.sortOrder() == null
                        ? segmentStore.findAll(new TripId(tripId)).stream()
                                        .mapToInt(TripSegment::sortOrder)
                                        .max()
                                        .orElse(-1)
                                + 1
                        : command.sortOrder();
        Instant now = clock.instant();
        TripSegment segment =
                TripSegment.create(
                        command.requestId(),
                        new TripId(tripId),
                        type(command.type()),
                        command.cityName(),
                        command.countryCode(),
                        command.placeId(),
                        command.latitude(),
                        command.longitude(),
                        command.startDate(),
                        command.endDate(),
                        command.accommodationName(),
                        command.accommodationPlaceId(),
                        command.checkInAt(),
                        command.checkOutAt(),
                        command.transportMode(),
                        command.departureAt(),
                        command.arrivalAt(),
                        sortOrder,
                        actorUserId,
                        now);
        TripSegment saved = segmentStore.save(segment);
        changes.publish(tripId, actorUserId, "CREATED", "TRIP_SEGMENT", saved.id());
        return result(saved);
    }

    @Override
    public SegmentResult update(
            UUID tripId, UUID segmentId, UUID actorUserId, SegmentCommand command) {
        tripAccess.requireEditor(tripId, actorUserId);
        TripSegment segment = loadInTrip(tripId, segmentId);
        verifyVersion(segment, command.baseVersion());
        segment.update(
                type(command.type()),
                command.cityName(),
                command.countryCode(),
                command.placeId(),
                command.latitude(),
                command.longitude(),
                command.startDate(),
                command.endDate(),
                command.accommodationName(),
                command.accommodationPlaceId(),
                command.checkInAt(),
                command.checkOutAt(),
                command.transportMode(),
                command.departureAt(),
                command.arrivalAt(),
                command.sortOrder() == null ? segment.sortOrder() : command.sortOrder(),
                actorUserId,
                clock.instant());
        TripSegment saved = segmentStore.save(segment);
        changes.publish(tripId, actorUserId, "UPDATED", "TRIP_SEGMENT", segmentId);
        return result(saved);
    }

    @Override
    public void delete(UUID tripId, UUID segmentId, UUID actorUserId, long baseVersion) {
        tripAccess.requireEditor(tripId, actorUserId);
        TripSegment segment = loadInTrip(tripId, segmentId);
        verifyVersion(segment, baseVersion);
        segmentStore.delete(segmentId);
        changes.publish(tripId, actorUserId, "DELETED", "TRIP_SEGMENT", segmentId);
    }

    @Override
    public List<SegmentResult> reorder(UUID tripId, UUID actorUserId, List<OrderItem> order) {
        tripAccess.requireEditor(tripId, actorUserId);
        List<TripSegment> current = segmentStore.findAll(new TripId(tripId));
        if (order.size() != current.size()
                || order.stream().map(OrderItem::segmentId).distinct().count() != current.size()) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_ORDER", "모든 구간을 중복 없이 포함해야 합니다.");
        }
        Instant now = clock.instant();
        for (OrderItem item : order) {
            TripSegment segment =
                    current.stream()
                            .filter(it -> it.id().equals(item.segmentId()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            EarthTripException.badRequest(
                                                    "INVALID_SEGMENT_ORDER", "다른 여행의 구간이 포함됐습니다."));
            verifyVersion(segment, item.baseVersion());
            segment.moveTo(item.sortOrder(), actorUserId, now);
            segmentStore.save(segment);
        }
        changes.publish(tripId, actorUserId, "REORDERED", "TRIP", tripId);
        return segmentStore.findAll(new TripId(tripId)).stream()
                .sorted(Comparator.comparingInt(TripSegment::sortOrder))
                .map(TripSegmentService::result)
                .toList();
    }

    private TripSegment loadInTrip(UUID tripId, UUID segmentId) {
        return segmentStore
                .findById(segmentId)
                .filter(it -> it.tripId().value().equals(tripId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "SEGMENT_NOT_FOUND", "여행 구간을 찾을 수 없습니다."));
    }

    private static TripSegment.Type type(String raw) {
        if (raw == null)
            throw EarthTripException.badRequest("SEGMENT_TYPE_REQUIRED", "구간 유형이 필요합니다.");
        try {
            return TripSegment.Type.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_TYPE", "지원하지 않는 구간 유형입니다.");
        }
    }

    private static void verifyVersion(TripSegment segment, long baseVersion) {
        if (segment.version() != baseVersion)
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 구간 변경이 먼저 저장되었습니다.",
                    java.util.Map.of("serverVersion", segment.version()));
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
    }

    private static SegmentResult result(TripSegment s) {
        return new SegmentResult(
                s.id(),
                s.tripId().value(),
                s.type().name(),
                s.cityName(),
                s.countryCode(),
                s.placeId(),
                s.latitude(),
                s.longitude(),
                s.startDate(),
                s.endDate(),
                s.accommodationName(),
                s.accommodationPlaceId(),
                s.checkInAt(),
                s.checkOutAt(),
                s.transportMode(),
                s.departureAt(),
                s.arrivalAt(),
                s.sortOrder(),
                s.version(),
                s.updatedBy(),
                s.updatedAt());
    }
}
