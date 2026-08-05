package com.earthtrip.trip.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripStructureUseCase {

    PreviewResult preview(UUID tripId, UUID actorUserId, StructureProposal proposal);

    ChangeSetResult apply(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal,
        String expectedProposalHash
    );

    ChangeSetResult synchronize(
        UUID tripId,
        UUID actorUserId,
        StructureProposal proposal
    );

    ChangeSetResult revert(
        UUID tripId,
        UUID changeSetId,
        UUID actorUserId,
        long baseVersion
    );

    List<DiagnosticResult> diagnostics(UUID tripId, UUID actorUserId);

    DiagnosticResult resolve(
        UUID tripId,
        UUID diagnosticId,
        UUID actorUserId,
        String note
    );

    void reopenDiagnostic(UUID tripId, UUID diagnosticId, UUID actorUserId);

    record StructureProposal(
        UUID requestId,
        long tripBaseVersion,
        LocalDate startDate,
        LocalDate endDate,
        List<SegmentProposal> segments,
        List<SegmentRemoval> removedSegments
    ) { }

    record SegmentRemoval(UUID segmentId, long baseVersion) { }

    record SegmentProposal(
        UUID segmentId,
        String type,
        String cityName,
        String countryCode,
        String placeId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        String accommodationName,
        String accommodationPlaceId,
        Instant checkInAt,
        Instant checkOutAt,
        String transportMode,
        Instant departureAt,
        Instant arrivalAt,
        int sortOrder,
        long baseVersion
    ) { }

    record PreviewResult(
        String proposalHash,
        List<UUID> addedSegmentIds,
        List<UUID> updatedSegmentIds,
        List<UUID> deletedSegmentIds,
        boolean dateRangeChanged,
        boolean reservationReviewRequired,
        List<DiagnosticResult> diagnostics
    ) { }

    record ChangeSetResult(
        UUID changeSetId,
        UUID tripId,
        String proposalHash,
        String status,
        Instant appliedAt,
        Instant revertedAt,
        long version
    ) { }

    record DiagnosticResult(
        UUID diagnosticId,
        String code,
        String severity,
        String message,
        LocalDate localDate,
        List<UUID> segmentIds,
        boolean resolved,
        String resolutionNote,
        UUID resolvedBy,
        Instant resolvedAt
    ) { }
}
