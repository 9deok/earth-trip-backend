package com.earthtrip.planning.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleChangeStorePort {

    Optional<ChangeSetRecord> findChangeSet(UUID changeSetId);

    ChangeSetRecord saveChangeSet(ChangeSetRecord record);

    List<ResolutionRecord> findResolutions(UUID tripId, UUID dayId);

    Optional<ResolutionRecord> findResolution(UUID diagnosticId);

    ResolutionRecord saveResolution(ResolutionRecord record);

    void deleteResolution(UUID diagnosticId);

    record ChangeSetRecord(
        UUID id,
        UUID tripId,
        UUID dayId,
        UUID requestedBy,
        String beforeSnapshot,
        String afterSnapshot,
        String status,
        Instant appliedAt,
        Instant revertedAt,
        long version
    ) { }

    record ResolutionRecord(
        UUID diagnosticId,
        UUID tripId,
        UUID dayId,
        String note,
        UUID resolvedBy,
        Instant resolvedAt
    ) { }
}
