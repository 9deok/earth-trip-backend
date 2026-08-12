package com.earthtrip.trip.application.service.structure;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import com.earthtrip.trip.application.port.out.TripStructureSerializationPort;
import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private final TripStructureSerializationPort serialization;
    private final Clock clock;

    TripStructureService(
            TripAccess access,
            TripStructureView structure,
            TripManagementUseCase trips,
            TripSegmentUseCase segments,
            TripStructureStorePort store,
            TripStructureSerializationPort serialization,
            Clock clock) {
        this.access = access;
        this.structure = structure;
        this.trips = trips;
        this.segments = segments;
        this.store = store;
        this.serialization = serialization;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(UUID tripId, UUID actorUserId, StructureProposal proposal) {
        access.requireEditor(tripId, actorUserId);
        StructureProposal safe = TripStructureProposalPolicy.validate(proposal);
        TripStructureView.StructureSnapshot current = structure.snapshot(tripId, actorUserId);
        TripStructureProposalPolicy.requireTripVersion(current.trip(), safe.tripBaseVersion());
        TripStructureProjection.Projected projected =
                TripStructureProjection.project(current, safe);
        return new PreviewResult(
                serialization.proposalHash(safe),
                projected.added(),
                projected.updated(),
                projected.deleted(),
                !Objects.equals(current.trip().startDate(), projected.startDate())
                        || !Objects.equals(current.trip().endDate(), projected.endDate()),
                TripStructureProjection.reservationReviewRequired(
                        current.segments(), projected.updated(), projected.deleted()),
                calculateDiagnostics(
                        tripId, projected.startDate(), projected.endDate(), projected.segments()));
    }

    @Override
    public ChangeSetResult apply(
            UUID tripId,
            UUID actorUserId,
            StructureProposal proposal,
            String expectedProposalHash) {
        PreviewResult preview = preview(tripId, actorUserId, proposal);
        if (!preview.proposalHash()
                .equals(TripStructureProposalPolicy.normalizeHash(expectedProposalHash))) {
            throw EarthTripException.conflict(
                    "STRUCTURE_PREVIEW_STALE", "검토한 구조 변경안과 적용 요청이 다릅니다. 다시 미리보기를 확인해 주세요.");
        }
        TripStructureStorePort.ChangeSetRecord existing =
                store.changeSet(proposal.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                    || !existing.proposalHash().equals(preview.proposalHash())) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 구조 변경에 사용된 요청 ID입니다.");
            }
            return result(existing);
        }

        TripStructureView.StructureSnapshot before = structure.snapshot(tripId, actorUserId);
        applyTripDateChange(tripId, actorUserId, proposal, before.trip());
        applySegmentChanges(tripId, actorUserId, proposal, before.segments());
        TripStructureView.StructureSnapshot after = structure.snapshot(tripId, actorUserId);
        Instant now = clock.instant();
        return result(
                store.saveChangeSet(
                        new TripStructureStorePort.ChangeSetRecord(
                                proposal.requestId(),
                                tripId,
                                actorUserId,
                                preview.proposalHash(),
                                serialization.serialize(before),
                                serialization.serialize(after),
                                "APPLIED",
                                now,
                                null,
                                0)));
    }

    @Override
    public ChangeSetResult synchronize(UUID tripId, UUID actorUserId, StructureProposal proposal) {
        PreviewResult preview = preview(tripId, actorUserId, proposal);
        return apply(tripId, actorUserId, proposal, preview.proposalHash());
    }

    @Override
    public ChangeSetResult revert(
            UUID tripId, UUID changeSetId, UUID actorUserId, long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        TripStructureStorePort.ChangeSetRecord record =
                store.changeSet(changeSetId)
                        .filter(candidate -> candidate.tripId().equals(tripId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "STRUCTURE_CHANGESET_NOT_FOUND",
                                                "구조 변경 이력을 찾을 수 없습니다."));
        TripStructureProposalPolicy.requireChangeSetVersion(record, baseVersion);
        if (record.status().equals("REVERTED")) {
            return result(record);
        }
        TripStructureView.StructureSnapshot expectedCurrent =
                serialization.deserialize(record.afterSnapshot());
        TripStructureView.StructureSnapshot current = structure.snapshot(tripId, actorUserId);
        if (!TripStructureProjection.sameVersions(current, expectedCurrent)) {
            throw EarthTripException.conflict(
                    "STRUCTURE_CHANGED_AFTER_CHANGESET", "적용 이후 구조가 다시 변경되어 자동으로 되돌릴 수 없습니다.");
        }
        TripStructureView.StructureSnapshot before =
                serialization.deserialize(record.beforeSnapshot());
        restoreSnapshot(tripId, actorUserId, current, before);
        Instant revertedAt = clock.instant();
        return result(
                store.saveChangeSet(
                        new TripStructureStorePort.ChangeSetRecord(
                                record.id(),
                                record.tripId(),
                                record.requestedBy(),
                                record.proposalHash(),
                                record.beforeSnapshot(),
                                record.afterSnapshot(),
                                "REVERTED",
                                record.appliedAt(),
                                revertedAt,
                                record.version())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticResult> diagnostics(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        TripStructureView.StructureSnapshot snapshot = structure.snapshot(tripId, actorUserId);
        return calculateDiagnostics(
                tripId,
                snapshot.trip().startDate(),
                snapshot.trip().endDate(),
                snapshot.segments());
    }

    @Override
    public DiagnosticResult resolve(UUID tripId, UUID diagnosticId, UUID actorUserId, String note) {
        access.requireEditor(tripId, actorUserId);
        DiagnosticResult diagnostic =
                diagnostics(tripId, actorUserId).stream()
                        .filter(candidate -> candidate.diagnosticId().equals(diagnosticId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "STRUCTURE_DIAGNOSTIC_NOT_FOUND",
                                                "현재 여행 구조에서 해당 진단을 찾을 수 없습니다."));
        String safeNote = TripStructureProposalPolicy.normalizeNote(note);
        TripStructureStorePort.ResolutionRecord resolution =
                store.saveResolution(
                        new TripStructureStorePort.ResolutionRecord(
                                diagnosticId, tripId, safeNote, actorUserId, clock.instant()));
        return new DiagnosticResult(
                diagnostic.diagnosticId(),
                diagnostic.code(),
                diagnostic.severity(),
                diagnostic.message(),
                diagnostic.localDate(),
                diagnostic.segmentIds(),
                true,
                resolution.note(),
                resolution.resolvedBy(),
                resolution.resolvedAt());
    }

    @Override
    public void reopenDiagnostic(UUID tripId, UUID diagnosticId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        TripStructureStorePort.ResolutionRecord resolution =
                store.resolution(diagnosticId)
                        .filter(candidate -> candidate.tripId().equals(tripId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "STRUCTURE_DIAGNOSTIC_RESOLUTION_NOT_FOUND",
                                                "해제할 구조 진단 확인 기록이 없습니다."));
        store.deleteResolution(resolution.diagnosticId());
    }

    private void applyTripDateChange(
            UUID tripId,
            UUID actorUserId,
            StructureProposal proposal,
            TripStructureView.Trip current) {
        LocalDate startDate =
                proposal.startDate() == null ? current.startDate() : proposal.startDate();
        LocalDate endDate = proposal.endDate() == null ? current.endDate() : proposal.endDate();
        if (Objects.equals(startDate, current.startDate())
                && Objects.equals(endDate, current.endDate())) {
            return;
        }
        trips.update(
                tripId,
                actorUserId,
                new TripManagementUseCase.UpdateTripCommand(
                        current.title(),
                        current.status(),
                        startDate,
                        endDate,
                        current.timeZone(),
                        current.defaultCurrency(),
                        current.planningMode(),
                        current.pace(),
                        current.version()));
    }

    private void applySegmentChanges(
            UUID tripId,
            UUID actorUserId,
            StructureProposal proposal,
            List<TripStructureView.Segment> currentSegments) {
        Map<UUID, TripStructureView.Segment> current =
                currentSegments.stream()
                        .collect(
                                Collectors.toMap(
                                        TripStructureView.Segment::segmentId, Function.identity()));
        for (SegmentRemoval removal : proposal.removedSegments()) {
            TripStructureView.Segment segment =
                    TripStructureProposalPolicy.requireCurrent(current, removal.segmentId());
            TripStructureProposalPolicy.requireSegmentVersion(segment, removal.baseVersion());
            segments.delete(tripId, segment.segmentId(), actorUserId, removal.baseVersion());
        }
        for (SegmentProposal proposed : proposal.segments()) {
            TripStructureView.Segment existing = current.get(proposed.segmentId());
            if (existing == null) {
                segments.create(tripId, actorUserId, TripStructureProjection.command(proposed, 0));
            } else {
                TripStructureProposalPolicy.requireSegmentVersion(existing, proposed.baseVersion());
                if (!TripStructureProjection.sameSegment(existing, proposed)) {
                    segments.update(
                            tripId,
                            proposed.segmentId(),
                            actorUserId,
                            TripStructureProjection.command(proposed, proposed.baseVersion()));
                }
            }
        }
    }

    private void restoreSnapshot(
            UUID tripId,
            UUID actorUserId,
            TripStructureView.StructureSnapshot current,
            TripStructureView.StructureSnapshot target) {
        if (!Objects.equals(current.trip().startDate(), target.trip().startDate())
                || !Objects.equals(current.trip().endDate(), target.trip().endDate())) {
            trips.update(
                    tripId,
                    actorUserId,
                    new TripManagementUseCase.UpdateTripCommand(
                            current.trip().title(),
                            current.trip().status(),
                            target.trip().startDate(),
                            target.trip().endDate(),
                            current.trip().timeZone(),
                            current.trip().defaultCurrency(),
                            current.trip().planningMode(),
                            current.trip().pace(),
                            current.trip().version()));
        }
        Map<UUID, TripStructureView.Segment> currentById =
                current.segments().stream()
                        .collect(
                                Collectors.toMap(
                                        TripStructureView.Segment::segmentId, Function.identity()));
        Map<UUID, TripStructureView.Segment> targetById =
                target.segments().stream()
                        .collect(
                                Collectors.toMap(
                                        TripStructureView.Segment::segmentId, Function.identity()));
        for (TripStructureView.Segment segment : current.segments()) {
            if (!targetById.containsKey(segment.segmentId())) {
                segments.delete(tripId, segment.segmentId(), actorUserId, segment.version());
            }
        }
        for (TripStructureView.Segment targetSegment : target.segments()) {
            TripStructureView.Segment existing = currentById.get(targetSegment.segmentId());
            SegmentProposal proposal =
                    TripStructureProjection.proposal(
                            targetSegment, existing == null ? 0 : existing.version());
            if (existing == null) {
                segments.create(tripId, actorUserId, TripStructureProjection.command(proposal, 0));
            } else if (!TripStructureProjection.sameSegment(existing, proposal)) {
                segments.update(
                        tripId,
                        existing.segmentId(),
                        actorUserId,
                        TripStructureProjection.command(proposal, existing.version()));
            }
        }
    }

    private List<DiagnosticResult> calculateDiagnostics(
            UUID tripId,
            LocalDate startDate,
            LocalDate endDate,
            List<TripStructureView.Segment> segments) {
        Map<UUID, TripStructureStorePort.ResolutionRecord> resolutions =
                store.resolutions(tripId).stream()
                        .collect(
                                Collectors.toMap(
                                        TripStructureStorePort.ResolutionRecord::diagnosticId,
                                        Function.identity()));
        return StructureDiagnosticCalculator.calculate(
                tripId, startDate, endDate, segments, resolutions);
    }

    private static Set<UUID> uniqueSegmentIds(List<UUID> ids) {
        if (ids.stream().anyMatch(Objects::isNull)
                || ids.stream().distinct().count() != ids.size()) {
            throw EarthTripException.badRequest(
                    "DUPLICATE_SEGMENT_CHANGE", "구조 변경 목록에 구간 ID가 중복되거나 비어 있습니다.");
        }
        return Set.copyOf(ids);
    }

    private static TripStructureView.Segment requireCurrent(
            Map<UUID, TripStructureView.Segment> current, UUID segmentId) {
        TripStructureView.Segment segment = current.get(segmentId);
        if (segment == null) {
            throw EarthTripException.notFound("SEGMENT_NOT_FOUND", "구조 변경 대상 구간을 찾을 수 없습니다.");
        }
        return segment;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            throw EarthTripException.badRequest("SEGMENT_TYPE_REQUIRED", "구간 유형이 필요합니다.");
        }
        String normalized = type.strip().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("STAY", "TRANSFER", "OVERNIGHT_TRANSFER").contains(normalized)) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_TYPE", "지원하지 않는 구간 유형입니다.");
        }
        return normalized;
    }

    private static void validateSegmentProposal(SegmentProposal proposal) {
        normalizeType(proposal.type());
        if (proposal.startDate() == null
                || proposal.endDate() == null
                || proposal.endDate().isBefore(proposal.startDate())) {
            throw EarthTripException.badRequest("INVALID_SEGMENT_DATES", "구간 시작일과 종료일을 확인해 주세요.");
        }
        if (normalizeType(proposal.type()).equals("STAY")
                && (proposal.cityName() == null || proposal.cityName().isBlank())) {
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

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw EarthTripException.badRequest("INVALID_TRIP_DATES", "여행 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private static void requireTripVersion(TripStructureView.Trip trip, long baseVersion) {
        if (trip.version() != baseVersion) {
            throw versionConflict("trip", trip.version());
        }
    }

    private static void requireSegmentVersion(TripStructureView.Segment segment, long baseVersion) {
        if (segment.version() != baseVersion) {
            throw versionConflict(segment.segmentId().toString(), segment.version());
        }
    }

    private static void requireChangeSetVersion(
            TripStructureStorePort.ChangeSetRecord record, long baseVersion) {
        if (record.version() != baseVersion) {
            throw versionConflict(record.id().toString(), record.version());
        }
    }

    private static EarthTripException versionConflict(String resourceId, long serverVersion) {
        return new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 구조 변경이 먼저 저장되었습니다.",
                Map.of("resourceId", resourceId, "serverVersion", serverVersion));
    }

    private static String normalizeHash(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw EarthTripException.badRequest(
                    "INVALID_PROPOSAL_HASH", "미리보기에서 받은 SHA-256 변경안 해시가 필요합니다.");
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
                    "RESOLUTION_NOTE_TOO_LONG", "확인 메모는 1000자 이하여야 합니다.");
        }
        return normalized;
    }

    private static ChangeSetResult result(TripStructureStorePort.ChangeSetRecord record) {
        return new ChangeSetResult(
                record.id(),
                record.tripId(),
                record.proposalHash(),
                record.status(),
                record.appliedAt(),
                record.revertedAt(),
                record.version());
    }
}
