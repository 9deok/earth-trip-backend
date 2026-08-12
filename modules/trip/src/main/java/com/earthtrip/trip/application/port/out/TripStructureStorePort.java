package com.earthtrip.trip.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripStructureStorePort {

    Optional<ChangeSetRecord> changeSet(UUID changeSetId);

    ChangeSetRecord saveChangeSet(ChangeSetRecord record);

    List<ResolutionRecord> resolutions(UUID tripId);

    Optional<ResolutionRecord> resolution(UUID diagnosticId);

    ResolutionRecord saveResolution(ResolutionRecord record);

    void deleteResolution(UUID diagnosticId);

    record ChangeSetRecord(
            UUID id,
            UUID tripId,
            UUID requestedBy,
            String proposalHash,
            String beforeSnapshot,
            String afterSnapshot,
            String status,
            Instant appliedAt,
            Instant revertedAt,
            long version) {}

    record ResolutionRecord(
            UUID diagnosticId, UUID tripId, String note, UUID resolvedBy, Instant resolvedAt) {}
}
