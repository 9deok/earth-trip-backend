package com.earthtrip.planning.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlanningCollectionUseCase {

    BatchResult createResearchSourceBatch(
            UUID tripId, UUID actorUserId, List<ResearchSourceItem> items);

    List<DuplicateResult> researchSourceDuplicates(
            UUID tripId, UUID actorUserId, DuplicateQuery query);

    List<DuplicateResult> placeCandidateDuplicates(
            UUID tripId, UUID actorUserId, DuplicateQuery query);

    record ResearchSourceItem(
            UUID requestId, UUID categoryId, Map<String, Object> payload, Integer sortOrder) {}

    record BatchItemResult(
            int index,
            UUID requestId,
            boolean succeeded,
            PlanningResourceUseCase.ResourceResult resource,
            String errorCode,
            String errorMessage) {}

    record BatchResult(
            int totalCount, int successCount, int failureCount, List<BatchItemResult> items) {}

    record DuplicateQuery(UUID anchorId, Map<String, Object> payload, Double minimumScore) {}

    record DuplicateResult(
            UUID resourceId,
            double score,
            List<String> reasons,
            PlanningResourceUseCase.ResourceResult resource) {}
}
