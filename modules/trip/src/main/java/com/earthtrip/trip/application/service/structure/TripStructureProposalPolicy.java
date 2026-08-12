package com.earthtrip.trip.application.service.structure;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.SegmentProposal;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.SegmentRemoval;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.StructureProposal;
import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class TripStructureProposalPolicy {
    private TripStructureProposalPolicy() {}

    static StructureProposal validate(StructureProposal proposal) {
        if (proposal == null || proposal.requestId() == null) {
            throw EarthTripException.badRequest(
                    "STRUCTURE_REQUEST_ID_REQUIRED", "구조 변경 요청 ID가 필요합니다.");
        }
        List<SegmentProposal> proposed =
                proposal.segments() == null ? List.of() : List.copyOf(proposal.segments());
        List<SegmentRemoval> removed =
                proposal.removedSegments() == null
                        ? List.of()
                        : List.copyOf(proposal.removedSegments());
        if (proposed.stream().anyMatch(Objects::isNull)
                || removed.stream().anyMatch(Objects::isNull)) {
            throw EarthTripException.badRequest(
                    "INVALID_STRUCTURE_PROPOSAL", "구조 변경 목록에 빈 항목이 포함될 수 없습니다.");
        }
        proposed.forEach(TripStructureProposalPolicy::validateSegment);
        Set<UUID> proposedIds =
                uniqueIds(proposed.stream().map(SegmentProposal::segmentId).toList());
        Set<UUID> removedIds = uniqueIds(removed.stream().map(SegmentRemoval::segmentId).toList());
        if (!java.util.Collections.disjoint(proposedIds, removedIds)) {
            throw EarthTripException.badRequest(
                    "CONTRADICTORY_SEGMENT_CHANGE", "같은 구간을 변경과 삭제에 동시에 포함할 수 없습니다.");
        }
        long distinctOrders = proposed.stream().map(SegmentProposal::sortOrder).distinct().count();
        if (distinctOrders != proposed.size()
                || proposed.stream().anyMatch(item -> item.sortOrder() < 0)) {
            throw EarthTripException.badRequest(
                    "INVALID_SEGMENT_ORDER", "변경 구간의 정렬 순서는 0 이상이며 서로 달라야 합니다.");
        }
        validateDateRange(proposal.startDate(), proposal.endDate());
        return new StructureProposal(
                proposal.requestId(),
                proposal.tripBaseVersion(),
                proposal.startDate(),
                proposal.endDate(),
                proposed.stream()
                        .sorted(Comparator.comparing(item -> item.segmentId().toString()))
                        .toList(),
                removed.stream()
                        .sorted(Comparator.comparing(item -> item.segmentId().toString()))
                        .toList());
    }

    static TripStructureView.Segment requireCurrent(
            Map<UUID, TripStructureView.Segment> current, UUID segmentId) {
        TripStructureView.Segment segment = current.get(segmentId);
        if (segment == null) {
            throw EarthTripException.notFound("SEGMENT_NOT_FOUND", "구조 변경 대상 구간을 찾을 수 없습니다.");
        }
        return segment;
    }

    static String normalizeType(String type) {
        if (type == null) {
            throw EarthTripException.badRequest("SEGMENT_TYPE_REQUIRED", "구간 유형이 필요합니다.");
        }
        String normalized = type.strip().toUpperCase(Locale.ROOT);
        if (!Set.of("STAY", "TRANSFER", "OVERNIGHT_TRANSFER").contains(normalized)) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_TYPE", "지원하지 않는 구간 유형입니다.");
        }
        return normalized;
    }

    static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw EarthTripException.badRequest("INVALID_TRIP_DATES", "여행 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    static void requireTripVersion(TripStructureView.Trip trip, long baseVersion) {
        if (trip.version() != baseVersion) {
            throw versionConflict("trip", trip.version());
        }
    }

    static void requireSegmentVersion(TripStructureView.Segment segment, long baseVersion) {
        if (segment.version() != baseVersion) {
            throw versionConflict(segment.segmentId().toString(), segment.version());
        }
    }

    static void requireChangeSetVersion(
            TripStructureStorePort.ChangeSetRecord record, long baseVersion) {
        if (record.version() != baseVersion) {
            throw versionConflict(record.id().toString(), record.version());
        }
    }

    static String normalizeHash(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw EarthTripException.badRequest(
                    "INVALID_PROPOSAL_HASH", "미리보기에서 받은 SHA-256 변경안 해시가 필요합니다.");
        }
        return normalized;
    }

    static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.strip();
        if (normalized.length() > 1_000) {
            throw EarthTripException.badRequest(
                    "RESOLUTION_NOTE_TOO_LONG", "확인 메모는 1000자 이하여야 합니다.");
        }
        return normalized;
    }

    private static void validateSegment(SegmentProposal proposal) {
        String type = normalizeType(proposal.type());
        if (proposal.startDate() == null
                || proposal.endDate() == null
                || proposal.endDate().isBefore(proposal.startDate())) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_DATES", "구간 시작일과 종료일을 확인해 주세요.");
        }
        if (type.equals("STAY") && (proposal.cityName() == null || proposal.cityName().isBlank())) {
            throw EarthTripException.badRequest("SEGMENT_CITY_REQUIRED", "체류 구간에는 도시 이름이 필요합니다.");
        }
        if (proposal.checkInAt() != null
                && proposal.checkOutAt() != null
                && proposal.checkOutAt().isBefore(proposal.checkInAt())) {
            throw EarthTripException.badRequest(
                    "INVALID_CHECK_IN_RANGE", "체크아웃 시각은 체크인보다 빠를 수 없습니다.");
        }
        if (proposal.departureAt() != null
                && proposal.arrivalAt() != null
                && proposal.arrivalAt().isBefore(proposal.departureAt())) {
            throw EarthTripException.badRequest(
                    "INVALID_TRANSFER_RANGE", "도착 시각은 출발 시각보다 빠를 수 없습니다.");
        }
        if (proposal.latitude() != null
                && (proposal.latitude().doubleValue() < -90
                        || proposal.latitude().doubleValue() > 90)) {
            throw EarthTripException.badRequest("INVALID_LATITUDE", "위도는 -90에서 90 사이여야 합니다.");
        }
        if (proposal.longitude() != null
                && (proposal.longitude().doubleValue() < -180
                        || proposal.longitude().doubleValue() > 180)) {
            throw EarthTripException.badRequest("INVALID_LONGITUDE", "경도는 -180에서 180 사이여야 합니다.");
        }
    }

    private static Set<UUID> uniqueIds(List<UUID> ids) {
        if (ids.stream().anyMatch(Objects::isNull)
                || ids.stream().distinct().count() != ids.size()) {
            throw EarthTripException.badRequest(
                    "DUPLICATE_SEGMENT_CHANGE", "구조 변경 목록에 구간 ID가 중복되거나 비어 있습니다.");
        }
        return Set.copyOf(ids);
    }

    private static EarthTripException versionConflict(String resourceId, long serverVersion) {
        return new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 구조 변경이 먼저 저장되었습니다.",
                Map.of("resourceId", resourceId, "serverVersion", serverVersion));
    }
}
