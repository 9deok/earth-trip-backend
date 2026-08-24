package com.earthtrip.trip.application.service.structure;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.SegmentProposal;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.SegmentRemoval;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.StructureProposal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TripStructureProjection {
    private TripStructureProjection() {}

    static Projected project(
            TripStructureView.StructureSnapshot current, StructureProposal proposal) {
        Map<UUID, TripStructureView.Segment> projected =
                current.segments().stream()
                        .collect(
                                Collectors.toMap(
                                        TripStructureView.Segment::segmentId,
                                        Function.identity(),
                                        (left, right) -> left,
                                        LinkedHashMap::new));
        List<UUID> deleted = new ArrayList<>();
        for (SegmentRemoval removal : proposal.removedSegments()) {
            TripStructureView.Segment segment =
                    TripStructureProposalPolicy.requireCurrent(projected, removal.segmentId());
            TripStructureProposalPolicy.requireSegmentVersion(segment, removal.baseVersion());
            projected.remove(removal.segmentId());
            deleted.add(removal.segmentId());
        }
        List<UUID> added = new ArrayList<>();
        List<UUID> updated = new ArrayList<>();
        for (SegmentProposal candidate : proposal.segments()) {
            TripStructureView.Segment existing = projected.get(candidate.segmentId());
            if (existing == null) {
                added.add(candidate.segmentId());
            } else {
                TripStructureProposalPolicy.requireSegmentVersion(
                        existing, candidate.baseVersion());
                if (!sameSegment(existing, candidate)) {
                    updated.add(candidate.segmentId());
                }
            }
            projected.put(candidate.segmentId(), segment(candidate));
        }
        LocalDate startDate =
                proposal.startDate() == null ? current.trip().startDate() : proposal.startDate();
        LocalDate endDate =
                proposal.endDate() == null ? current.trip().endDate() : proposal.endDate();
        TripStructureProposalPolicy.validateDateRange(startDate, endDate);
        List<TripStructureView.Segment> ordered =
                projected.values().stream()
                        .sorted(Comparator.comparingInt(TripStructureView.Segment::sortOrder))
                        .toList();
        if (ordered.stream().map(TripStructureView.Segment::sortOrder).distinct().count()
                != ordered.size()) {
            throw EarthTripException.badRequest(
                    "INVALID_SEGMENT_ORDER", "적용 후 모든 구간의 정렬 순서는 서로 달라야 합니다.");
        }
        return new Projected(
                startDate,
                endDate,
                ordered,
                List.copyOf(added),
                List.copyOf(updated),
                List.copyOf(deleted));
    }

    static boolean sameVersions(
            TripStructureView.StructureSnapshot current,
            TripStructureView.StructureSnapshot expected) {
        if (current.trip().version() != expected.trip().version()) {
            return false;
        }
        Map<UUID, Long> currentVersions = versions(current);
        Map<UUID, Long> expectedVersions = versions(expected);
        return currentVersions.equals(expectedVersions);
    }

    static TripSegmentUseCase.SegmentCommand command(SegmentProposal proposal, long baseVersion) {
        return new TripSegmentUseCase.SegmentCommand(
                proposal.segmentId(),
                proposal.type(),
                proposal.cityName(),
                proposal.countryCode(),
                proposal.placeId(),
                proposal.latitude(),
                proposal.longitude(),
                proposal.timeZone(),
                proposal.startDate(),
                proposal.endDate(),
                proposal.accommodationName(),
                proposal.accommodationPlaceId(),
                proposal.checkInAt(),
                proposal.checkOutAt(),
                proposal.transportMode(),
                proposal.departureAt(),
                proposal.arrivalAt(),
                proposal.anchorAt(),
                proposal.sortOrder(),
                baseVersion);
    }

    static SegmentProposal proposal(TripStructureView.Segment segment, long baseVersion) {
        return new SegmentProposal(
                segment.segmentId(),
                segment.type(),
                segment.cityName(),
                segment.countryCode(),
                segment.placeId(),
                segment.latitude(),
                segment.longitude(),
                segment.timeZone(),
                segment.startDate(),
                segment.endDate(),
                segment.accommodationName(),
                segment.accommodationPlaceId(),
                segment.checkInAt(),
                segment.checkOutAt(),
                segment.transportMode(),
                segment.departureAt(),
                segment.arrivalAt(),
                segment.anchorAt(),
                segment.sortOrder(),
                baseVersion);
    }

    static boolean sameSegment(TripStructureView.Segment current, SegmentProposal proposed) {
        return current.type().equals(TripStructureProposalPolicy.normalizeType(proposed.type()))
                && Objects.equals(current.cityName(), proposed.cityName())
                && Objects.equals(current.countryCode(), proposed.countryCode())
                && Objects.equals(current.placeId(), proposed.placeId())
                && Objects.equals(current.latitude(), proposed.latitude())
                && Objects.equals(current.longitude(), proposed.longitude())
                && Objects.equals(current.timeZone(), proposed.timeZone())
                && Objects.equals(current.startDate(), proposed.startDate())
                && Objects.equals(current.endDate(), proposed.endDate())
                && Objects.equals(current.accommodationName(), proposed.accommodationName())
                && Objects.equals(current.accommodationPlaceId(), proposed.accommodationPlaceId())
                && Objects.equals(current.checkInAt(), proposed.checkInAt())
                && Objects.equals(current.checkOutAt(), proposed.checkOutAt())
                && Objects.equals(current.transportMode(), proposed.transportMode())
                && Objects.equals(current.departureAt(), proposed.departureAt())
                && Objects.equals(current.arrivalAt(), proposed.arrivalAt())
                && Objects.equals(current.anchorAt(), proposed.anchorAt())
                && current.sortOrder() == proposed.sortOrder();
    }

    static boolean reservationReviewRequired(
            List<TripStructureView.Segment> current, List<UUID> updated, List<UUID> deleted) {
        Set<UUID> affected =
                Stream.concat(updated.stream(), deleted.stream()).collect(Collectors.toSet());
        return current.stream()
                .filter(segment -> affected.contains(segment.segmentId()))
                .anyMatch(
                        segment ->
                                segment.accommodationName() != null
                                        || segment.transportMode() != null);
    }

    private static TripStructureView.Segment segment(SegmentProposal proposal) {
        return new TripStructureView.Segment(
                proposal.segmentId(),
                TripStructureProposalPolicy.normalizeType(proposal.type()),
                proposal.cityName(),
                proposal.countryCode(),
                proposal.placeId(),
                proposal.latitude(),
                proposal.longitude(),
                proposal.timeZone(),
                proposal.startDate(),
                proposal.endDate(),
                proposal.accommodationName(),
                proposal.accommodationPlaceId(),
                proposal.checkInAt(),
                proposal.checkOutAt(),
                proposal.transportMode(),
                proposal.departureAt(),
                proposal.arrivalAt(),
                proposal.anchorAt(),
                proposal.sortOrder(),
                proposal.baseVersion());
    }

    private static Map<UUID, Long> versions(TripStructureView.StructureSnapshot snapshot) {
        return snapshot.segments().stream()
                .collect(
                        Collectors.toMap(
                                TripStructureView.Segment::segmentId,
                                TripStructureView.Segment::version));
    }

    record Projected(
            LocalDate startDate,
            LocalDate endDate,
            List<TripStructureView.Segment> segments,
            List<UUID> added,
            List<UUID> updated,
            List<UUID> deleted) {}
}
