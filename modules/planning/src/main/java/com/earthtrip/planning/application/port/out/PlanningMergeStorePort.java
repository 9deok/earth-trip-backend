package com.earthtrip.planning.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlanningMergeStorePort {

    Optional<MergeRecord> find(UUID mergeId);

    MergeRecord save(MergeRecord merge);

    record MergeRecord(
            UUID id,
            UUID tripId,
            String resourceType,
            UUID primaryId,
            List<UUID> duplicateIds,
            Map<String, Object> beforeSnapshot,
            Map<String, Object> afterSnapshot,
            List<Map<String, Object>> addedLinks,
            String status,
            UUID mergedBy,
            Instant mergedAt,
            UUID revertedBy,
            Instant revertedAt,
            long version) {}
}
