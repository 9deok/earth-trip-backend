package com.earthtrip.trip.application.service.structure;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripStructureService implements TripStructureUseCase {

    private final TripAccess access;
    private final TripStructureView structure;
    private final TripManagementUseCase trips;
    private final TripSegmentUseCase segments;
    private final TripStructureStorePort store;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    TripStructureService(
        TripAccess access,
        TripStructureView structure,
        TripManagementUseCase trips,
        TripSegmentUseCase segments,
        TripStructureStorePort store,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.access = access;
        this.structure = structure;
        this.trips = trips;
        this.segments = segments;
        this.store = store;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal
    ) {
        access.requireEditor(tripId, actorUserId);
        StructureProposal safe = validateProposal(proposal);
        TripStructureView.StructureSnapshot current = structure.snapshot(tripId, actorUserId);
        requireTripVersion(current.trip(), safe.tripBaseVersion());
        Projected projected = project(current, safe);
        return new PreviewResult(
            hash(safe),
            projected.added(),
            projected.updated(),
            projected.deleted(),
            !Objects.equals(current.trip().startDate(), projected.startDate())
                || !Objects.equals(current.trip().endDate(), projected.endDate()),
            reservationReviewRequired(current.segments(), projected.updated(), projected.deleted()),
            calculateDiagnostics(
                tripId, projected.startDate(), projected.endDate(), projected.segments()
            )
        );
    }

    @Override
    public ChangeSetResult apply(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal,
        String expectedProposalHash
    ) {
        PreviewResult preview = preview(tripId, actorUserId, proposal);
        if (!preview.proposalHash().equals(normalizeHash(expectedProposalHash))) {
            throw EarthTripException.conflict(
                "STRUCTURE_PREVIEW_STALE",
                "검토한 구조 변경안과 적용 요청이 다릅니다. 다시 미리보기를 확인해 주세요."
            );
        }
        TripStructureStorePort.ChangeSetRecord existing = store.changeSet(
            proposal.requestId()
        ).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                || !existing.proposalHash().equals(preview.proposalHash())) {
                throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED",
                    "이미 다른 구조 변경에 사용된 요청 ID입니다."
                );
            }
            return result(existing);
        }

        TripStructureView.StructureSnapshot before = structure.snapshot(tripId, actorUserId);
        applyTripDateChange(tripId, actorUserId, proposal, before.trip());
        applySegmentChanges(tripId, actorUserId, proposal, before.segments());
        TripStructureView.StructureSnapshot after = structure.snapshot(tripId, actorUserId);
        Instant now = clock.instant();
        return result(store.saveChangeSet(new TripStructureStorePort.ChangeSetRecord(
            proposal.requestId(), tripId, actorUserId, preview.proposalHash(),
            write(before), write(after), "APPLIED", now, null, 0
        )));
    }

    @Override
    public ChangeSetResult revert(
        UUID tripId,
        UUID changeSetId,
        UUID actorUserId,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        TripStructureStorePort.ChangeSetRecord record = store.changeSet(changeSetId)
            .filter(candidate -> candidate.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "STRUCTURE_CHANGESET_NOT_FOUND",
                "구조 변경 이력을 찾을 수 없습니다."
            ));
        requireChangeSetVersion(record, baseVersion);
        if (record.status().equals("REVERTED")) {
            return result(record);
        }
        TripStructureView.StructureSnapshot expectedCurrent = read(record.afterSnapshot());
        TripStructureView.StructureSnapshot current = structure.snapshot(tripId, actorUserId);
        if (!sameVersions(current, expectedCurrent)) {
            throw EarthTripException.conflict(
                "STRUCTURE_CHANGED_AFTER_CHANGESET",
                "적용 이후 구조가 다시 변경되어 자동으로 되돌릴 수 없습니다."
            );
        }
        TripStructureView.StructureSnapshot before = read(record.beforeSnapshot());
        restoreSnapshot(tripId, actorUserId, current, before);
        Instant revertedAt = clock.instant();
        return result(store.saveChangeSet(new TripStructureStorePort.ChangeSetRecord(
            record.id(), record.tripId(), record.requestedBy(), record.proposalHash(),
            record.beforeSnapshot(), record.afterSnapshot(), "REVERTED", record.appliedAt(),
            revertedAt, record.version()
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticResult> diagnostics(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        TripStructureView.StructureSnapshot snapshot = structure.snapshot(tripId, actorUserId);
        return calculateDiagnostics(
            tripId, snapshot.trip().startDate(), snapshot.trip().endDate(), snapshot.segments()
        );
    }

    @Override
    public DiagnosticResult resolve(
        UUID tripId,
        UUID diagnosticId,
        UUID actorUserId,
        String note
    ) {
        access.requireEditor(tripId, actorUserId);
        DiagnosticResult diagnostic = diagnostics(tripId, actorUserId).stream()
            .filter(candidate -> candidate.diagnosticId().equals(diagnosticId))
            .findFirst()
            .orElseThrow(() -> EarthTripException.notFound(
                "STRUCTURE_DIAGNOSTIC_NOT_FOUND",
                "현재 여행 구조에서 해당 진단을 찾을 수 없습니다."
            ));
        String safeNote = normalizeNote(note);
        TripStructureStorePort.ResolutionRecord resolution = store.saveResolution(
            new TripStructureStorePort.ResolutionRecord(
                diagnosticId, tripId, safeNote, actorUserId, clock.instant()
            )
        );
        return new DiagnosticResult(
            diagnostic.diagnosticId(), diagnostic.code(), diagnostic.severity(),
            diagnostic.message(), diagnostic.localDate(), diagnostic.segmentIds(), true,
            resolution.note(), resolution.resolvedBy(), resolution.resolvedAt()
        );
    }

    @Override
    public void reopenDiagnostic(UUID tripId, UUID diagnosticId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        TripStructureStorePort.ResolutionRecord resolution = store.resolution(diagnosticId)
            .filter(candidate -> candidate.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "STRUCTURE_DIAGNOSTIC_RESOLUTION_NOT_FOUND",
                "해제할 구조 진단 확인 기록이 없습니다."
            ));
        store.deleteResolution(resolution.diagnosticId());
    }

    private void applyTripDateChange(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal,
        TripStructureView.Trip current
    ) {
        LocalDate startDate = proposal.startDate() == null
            ? current.startDate()
            : proposal.startDate();
        LocalDate endDate = proposal.endDate() == null
            ? current.endDate()
            : proposal.endDate();
        if (Objects.equals(startDate, current.startDate())
            && Objects.equals(endDate, current.endDate())) {
            return;
        }
        trips.update(tripId, actorUserId, new TripManagementUseCase.UpdateTripCommand(
            current.title(), current.status(), startDate, endDate, current.timeZone(),
            current.defaultCurrency(), current.planningMode(), current.pace(), current.version()
        ));
    }

    private void applySegmentChanges(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal,
        List<TripStructureView.Segment> currentSegments
    ) {
        Map<UUID, TripStructureView.Segment> current = currentSegments.stream()
            .collect(Collectors.toMap(TripStructureView.Segment::segmentId, Function.identity()));
        for (SegmentRemoval removal : proposal.removedSegments()) {
            TripStructureView.Segment segment = requireCurrent(current, removal.segmentId());
            requireSegmentVersion(segment, removal.baseVersion());
            segments.delete(tripId, segment.segmentId(), actorUserId, removal.baseVersion());
        }
        for (SegmentProposal proposed : proposal.segments()) {
            TripStructureView.Segment existing = current.get(proposed.segmentId());
            if (existing == null) {
                segments.create(tripId, actorUserId, command(proposed, 0));
            } else {
                requireSegmentVersion(existing, proposed.baseVersion());
                if (!sameSegment(existing, proposed)) {
                    segments.update(
                        tripId, proposed.segmentId(), actorUserId,
                        command(proposed, proposed.baseVersion())
                    );
                }
            }
        }
    }

    private void restoreSnapshot(
        UUID tripId,
        UUID actorUserId,
        TripStructureView.StructureSnapshot current,
        TripStructureView.StructureSnapshot target
    ) {
        if (!Objects.equals(current.trip().startDate(), target.trip().startDate())
            || !Objects.equals(current.trip().endDate(), target.trip().endDate())) {
            trips.update(tripId, actorUserId, new TripManagementUseCase.UpdateTripCommand(
                current.trip().title(), current.trip().status(), target.trip().startDate(),
                target.trip().endDate(), current.trip().timeZone(), current.trip().defaultCurrency(),
                current.trip().planningMode(), current.trip().pace(), current.trip().version()
            ));
        }
        Map<UUID, TripStructureView.Segment> currentById = current.segments().stream()
            .collect(Collectors.toMap(TripStructureView.Segment::segmentId, Function.identity()));
        Map<UUID, TripStructureView.Segment> targetById = target.segments().stream()
            .collect(Collectors.toMap(TripStructureView.Segment::segmentId, Function.identity()));
        for (TripStructureView.Segment segment : current.segments()) {
            if (!targetById.containsKey(segment.segmentId())) {
                segments.delete(tripId, segment.segmentId(), actorUserId, segment.version());
            }
        }
        for (TripStructureView.Segment targetSegment : target.segments()) {
            TripStructureView.Segment existing = currentById.get(targetSegment.segmentId());
            SegmentProposal proposal = proposal(targetSegment, existing == null ? 0 : existing.version());
            if (existing == null) {
                segments.create(tripId, actorUserId, command(proposal, 0));
            } else if (!sameSegment(existing, proposal)) {
                segments.update(
                    tripId, existing.segmentId(), actorUserId,
                    command(proposal, existing.version())
                );
            }
        }
    }

    private Projected project(
        TripStructureView.StructureSnapshot current,
        StructureProposal proposal
    ) {
        Map<UUID, TripStructureView.Segment> projected = current.segments().stream()
            .collect(Collectors.toMap(
                TripStructureView.Segment::segmentId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        List<UUID> deleted = new ArrayList<>();
        for (SegmentRemoval removal : proposal.removedSegments()) {
            TripStructureView.Segment segment = requireCurrent(projected, removal.segmentId());
            requireSegmentVersion(segment, removal.baseVersion());
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
                requireSegmentVersion(existing, candidate.baseVersion());
                if (!sameSegment(existing, candidate)) {
                    updated.add(candidate.segmentId());
                }
            }
            projected.put(candidate.segmentId(), segment(candidate));
        }
        LocalDate startDate = proposal.startDate() == null
            ? current.trip().startDate()
            : proposal.startDate();
        LocalDate endDate = proposal.endDate() == null
            ? current.trip().endDate()
            : proposal.endDate();
        validateDateRange(startDate, endDate);
        List<TripStructureView.Segment> ordered = projected.values().stream()
            .sorted(Comparator.comparingInt(TripStructureView.Segment::sortOrder))
            .toList();
        if (ordered.stream().map(TripStructureView.Segment::sortOrder).distinct().count()
            != ordered.size()) {
            throw EarthTripException.badRequest(
                "INVALID_SEGMENT_ORDER",
                "적용 후 모든 구간의 정렬 순서는 서로 달라야 합니다."
            );
        }
        return new Projected(
            startDate, endDate, ordered, List.copyOf(added),
            List.copyOf(updated), List.copyOf(deleted)
        );
    }

    private List<DiagnosticResult> calculateDiagnostics(
        UUID tripId,
        LocalDate startDate,
        LocalDate endDate,
        List<TripStructureView.Segment> segments
    ) {
        Map<UUID, TripStructureStorePort.ResolutionRecord> resolutions = store.resolutions(tripId)
            .stream()
            .collect(Collectors.toMap(
                TripStructureStorePort.ResolutionRecord::diagnosticId,
                Function.identity()
            ));
        return StructureDiagnosticCalculator.calculate(
            tripId, startDate, endDate, segments, resolutions
        );
    }

    private StructureProposal validateProposal(StructureProposal proposal) {
        if (proposal == null || proposal.requestId() == null) {
            throw EarthTripException.badRequest(
                "STRUCTURE_REQUEST_ID_REQUIRED",
                "구조 변경 요청 ID가 필요합니다."
            );
        }
        List<SegmentProposal> proposed = proposal.segments() == null
            ? List.of()
            : List.copyOf(proposal.segments());
        List<SegmentRemoval> removed = proposal.removedSegments() == null
            ? List.of()
            : List.copyOf(proposal.removedSegments());
        if (proposed.stream().anyMatch(Objects::isNull)
            || removed.stream().anyMatch(Objects::isNull)) {
            throw EarthTripException.badRequest(
                "INVALID_STRUCTURE_PROPOSAL",
                "구조 변경 목록에 빈 항목이 포함될 수 없습니다."
            );
        }
        proposed.forEach(TripStructureService::validateSegmentProposal);
        Set<UUID> proposedIds = uniqueSegmentIds(
            proposed.stream().map(SegmentProposal::segmentId).toList()
        );
        Set<UUID> removedIds = uniqueSegmentIds(
            removed.stream().map(SegmentRemoval::segmentId).toList()
        );
        if (!java.util.Collections.disjoint(proposedIds, removedIds)) {
            throw EarthTripException.badRequest(
                "CONTRADICTORY_SEGMENT_CHANGE",
                "같은 구간을 변경과 삭제에 동시에 포함할 수 없습니다."
            );
        }
        long distinctOrders = proposed.stream().map(SegmentProposal::sortOrder).distinct().count();
        if (distinctOrders != proposed.size() || proposed.stream().anyMatch(item -> item.sortOrder() < 0)) {
            throw EarthTripException.badRequest(
                "INVALID_SEGMENT_ORDER",
                "변경 구간의 정렬 순서는 0 이상이며 서로 달라야 합니다."
            );
        }
        validateDateRange(proposal.startDate(), proposal.endDate());
        return new StructureProposal(
            proposal.requestId(), proposal.tripBaseVersion(), proposal.startDate(), proposal.endDate(),
            proposed.stream().sorted(Comparator.comparing(item -> item.segmentId().toString())).toList(),
            removed.stream().sorted(Comparator.comparing(item -> item.segmentId().toString())).toList()
        );
    }

    private String hash(StructureProposal proposal) {
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(proposal);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(serialized));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("구조 변경안 해시를 만들 수 없습니다.", exception);
        }
    }

    private String write(TripStructureView.StructureSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("여행 구조 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    private TripStructureView.StructureSnapshot read(String json) {
        try {
            return objectMapper.readValue(json, TripStructureView.StructureSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 여행 구조 스냅샷을 읽을 수 없습니다.", exception);
        }
    }

    private static boolean sameVersions(
        TripStructureView.StructureSnapshot current,
        TripStructureView.StructureSnapshot expected
    ) {
        if (current.trip().version() != expected.trip().version()) {
            return false;
        }
        Map<UUID, Long> currentVersions = current.segments().stream()
            .collect(Collectors.toMap(
                TripStructureView.Segment::segmentId,
                TripStructureView.Segment::version
            ));
        Map<UUID, Long> expectedVersions = expected.segments().stream()
            .collect(Collectors.toMap(
                TripStructureView.Segment::segmentId,
                TripStructureView.Segment::version
            ));
        return currentVersions.equals(expectedVersions);
    }

    private static TripSegmentUseCase.SegmentCommand command(
        SegmentProposal proposal,
        long baseVersion
    ) {
        return new TripSegmentUseCase.SegmentCommand(
            proposal.segmentId(), proposal.type(), proposal.cityName(), proposal.countryCode(),
            proposal.placeId(), proposal.latitude(), proposal.longitude(), proposal.startDate(),
            proposal.endDate(), proposal.accommodationName(), proposal.accommodationPlaceId(),
            proposal.checkInAt(), proposal.checkOutAt(), proposal.transportMode(),
            proposal.departureAt(), proposal.arrivalAt(), proposal.sortOrder(), baseVersion
        );
    }

    private static SegmentProposal proposal(
        TripStructureView.Segment segment,
        long baseVersion
    ) {
        return new SegmentProposal(
            segment.segmentId(), segment.type(), segment.cityName(), segment.countryCode(),
            segment.placeId(), segment.latitude(), segment.longitude(), segment.startDate(),
            segment.endDate(), segment.accommodationName(), segment.accommodationPlaceId(),
            segment.checkInAt(), segment.checkOutAt(), segment.transportMode(),
            segment.departureAt(), segment.arrivalAt(), segment.sortOrder(), baseVersion
        );
    }

    private static TripStructureView.Segment segment(SegmentProposal proposal) {
        return new TripStructureView.Segment(
            proposal.segmentId(), normalizeType(proposal.type()), proposal.cityName(),
            proposal.countryCode(), proposal.placeId(), proposal.latitude(), proposal.longitude(),
            proposal.startDate(), proposal.endDate(), proposal.accommodationName(),
            proposal.accommodationPlaceId(), proposal.checkInAt(), proposal.checkOutAt(),
            proposal.transportMode(), proposal.departureAt(), proposal.arrivalAt(),
            proposal.sortOrder(), proposal.baseVersion()
        );
    }

    private static boolean sameSegment(
        TripStructureView.Segment current,
        SegmentProposal proposed
    ) {
        return current.type().equals(normalizeType(proposed.type()))
            && Objects.equals(current.cityName(), proposed.cityName())
            && Objects.equals(current.countryCode(), proposed.countryCode())
            && Objects.equals(current.placeId(), proposed.placeId())
            && Objects.equals(current.latitude(), proposed.latitude())
            && Objects.equals(current.longitude(), proposed.longitude())
            && Objects.equals(current.startDate(), proposed.startDate())
            && Objects.equals(current.endDate(), proposed.endDate())
            && Objects.equals(current.accommodationName(), proposed.accommodationName())
            && Objects.equals(current.accommodationPlaceId(), proposed.accommodationPlaceId())
            && Objects.equals(current.checkInAt(), proposed.checkInAt())
            && Objects.equals(current.checkOutAt(), proposed.checkOutAt())
            && Objects.equals(current.transportMode(), proposed.transportMode())
            && Objects.equals(current.departureAt(), proposed.departureAt())
            && Objects.equals(current.arrivalAt(), proposed.arrivalAt())
            && current.sortOrder() == proposed.sortOrder();
    }

    private static boolean reservationReviewRequired(
        List<TripStructureView.Segment> current,
        List<UUID> updated,
        List<UUID> deleted
    ) {
        Set<UUID> affected = java.util.stream.Stream.concat(updated.stream(), deleted.stream())
            .collect(Collectors.toSet());
        return current.stream()
            .filter(segment -> affected.contains(segment.segmentId()))
            .anyMatch(segment -> segment.accommodationName() != null
                || segment.transportMode() != null);
    }

    private static Set<UUID> uniqueSegmentIds(List<UUID> ids) {
        if (ids.stream().anyMatch(Objects::isNull) || ids.stream().distinct().count() != ids.size()) {
            throw EarthTripException.badRequest(
                "DUPLICATE_SEGMENT_CHANGE",
                "구조 변경 목록에 구간 ID가 중복되거나 비어 있습니다."
            );
        }
        return Set.copyOf(ids);
    }

    private static TripStructureView.Segment requireCurrent(
        Map<UUID, TripStructureView.Segment> current,
        UUID segmentId
    ) {
        TripStructureView.Segment segment = current.get(segmentId);
        if (segment == null) {
            throw EarthTripException.notFound(
                "SEGMENT_NOT_FOUND",
                "구조 변경 대상 구간을 찾을 수 없습니다."
            );
        }
        return segment;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            throw EarthTripException.badRequest("SEGMENT_TYPE_REQUIRED", "구간 유형이 필요합니다.");
        }
        String normalized = type.strip().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("STAY", "TRANSFER", "OVERNIGHT_TRANSFER").contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_SEGMENT_TYPE",
                "지원하지 않는 구간 유형입니다."
            );
        }
        return normalized;
    }

    private static void validateSegmentProposal(SegmentProposal proposal) {
        normalizeType(proposal.type());
        if (proposal.startDate() == null || proposal.endDate() == null
            || proposal.endDate().isBefore(proposal.startDate())) {
            throw EarthTripException.badRequest(
                "INVALID_SEGMENT_DATES",
                "구간 시작일과 종료일을 확인해 주세요."
            );
        }
        if (normalizeType(proposal.type()).equals("STAY")
            && (proposal.cityName() == null || proposal.cityName().isBlank())) {
            throw EarthTripException.badRequest(
                "SEGMENT_CITY_REQUIRED",
                "체류 구간에는 도시 이름이 필요합니다."
            );
        }
        if (proposal.checkInAt() != null && proposal.checkOutAt() != null
            && proposal.checkOutAt().isBefore(proposal.checkInAt())) {
            throw EarthTripException.badRequest(
                "INVALID_CHECK_IN_RANGE",
                "체크아웃 시각은 체크인보다 빠를 수 없습니다."
            );
        }
        if (proposal.departureAt() != null && proposal.arrivalAt() != null
            && proposal.arrivalAt().isBefore(proposal.departureAt())) {
            throw EarthTripException.badRequest(
                "INVALID_TRANSFER_RANGE",
                "도착 시각은 출발 시각보다 빠를 수 없습니다."
            );
        }
        if (proposal.latitude() != null
            && (proposal.latitude().doubleValue() < -90
                || proposal.latitude().doubleValue() > 90)) {
            throw EarthTripException.badRequest(
                "INVALID_LATITUDE",
                "위도는 -90에서 90 사이여야 합니다."
            );
        }
        if (proposal.longitude() != null
            && (proposal.longitude().doubleValue() < -180
                || proposal.longitude().doubleValue() > 180)) {
            throw EarthTripException.badRequest(
                "INVALID_LONGITUDE",
                "경도는 -180에서 180 사이여야 합니다."
            );
        }
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw EarthTripException.badRequest(
                "INVALID_TRIP_DATES",
                "여행 종료일은 시작일보다 빠를 수 없습니다."
            );
        }
    }

    private static void requireTripVersion(TripStructureView.Trip trip, long baseVersion) {
        if (trip.version() != baseVersion) {
            throw versionConflict("trip", trip.version());
        }
    }

    private static void requireSegmentVersion(
        TripStructureView.Segment segment,
        long baseVersion
    ) {
        if (segment.version() != baseVersion) {
            throw versionConflict(segment.segmentId().toString(), segment.version());
        }
    }

    private static void requireChangeSetVersion(
        TripStructureStorePort.ChangeSetRecord record,
        long baseVersion
    ) {
        if (record.version() != baseVersion) {
            throw versionConflict(record.id().toString(), record.version());
        }
    }

    private static EarthTripException versionConflict(String resourceId, long serverVersion) {
        return new EarthTripException(
            "VERSION_CONFLICT",
            409,
            "다른 구조 변경이 먼저 저장되었습니다.",
            Map.of("resourceId", resourceId, "serverVersion", serverVersion)
        );
    }

    private static String normalizeHash(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw EarthTripException.badRequest(
                "INVALID_PROPOSAL_HASH",
                "미리보기에서 받은 SHA-256 변경안 해시가 필요합니다."
            );
        }
        return normalized;
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.strip();
        if (normalized.length() > 1_000) {
            throw EarthTripException.badRequest(
                "RESOLUTION_NOTE_TOO_LONG",
                "확인 메모는 1000자 이하여야 합니다."
            );
        }
        return normalized;
    }

    private static ChangeSetResult result(TripStructureStorePort.ChangeSetRecord record) {
        return new ChangeSetResult(
            record.id(), record.tripId(), record.proposalHash(), record.status(),
            record.appliedAt(), record.revertedAt(), record.version()
        );
    }

    private record Projected(
        LocalDate startDate,
        LocalDate endDate,
        List<TripStructureView.Segment> segments,
        List<UUID> added,
        List<UUID> updated,
        List<UUID> deleted
    ) { }
}
