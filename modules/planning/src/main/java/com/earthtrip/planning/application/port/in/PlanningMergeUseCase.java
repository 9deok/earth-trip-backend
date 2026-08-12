package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlanningMergeUseCase {

    MergeResult mergeResearchSources(UUID tripId, UUID actorUserId, MergeCommand command);

    MergeResult mergePlaceCandidates(UUID tripId, UUID actorUserId, MergeCommand command);

    MergeResult revertResearchSourceMerge(UUID tripId, UUID mergeId, UUID actorUserId);

    MergeResult revertPlaceCandidateMerge(UUID tripId, UUID mergeId, UUID actorUserId);

    record MergeCommand(
            UUID requestId,
            UUID primaryId,
            List<UUID> duplicateIds,
            Map<String, Object> mergedPayload,
            long primaryBaseVersion,
            Map<UUID, Long> duplicateBaseVersions) {}

    record MergeResult(
            UUID mergeId,
            String resourceType,
            String status,
            PlanningResourceUseCase.ResourceResult primary,
            List<PlanningResourceUseCase.ResourceResult> duplicates,
            Instant mergedAt,
            Instant revertedAt) {}
}
