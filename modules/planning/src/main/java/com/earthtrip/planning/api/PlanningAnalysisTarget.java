package com.earthtrip.planning.api;

import java.util.Map;
import java.util.UUID;

public interface PlanningAnalysisTarget {

    TargetResult getResearchSource(UUID tripId, UUID sourceId, UUID actorUserId);

    TargetResult confirmResearchSource(
            UUID tripId,
            UUID sourceId,
            UUID actorUserId,
            Map<String, Object> confirmedFields,
            long baseVersion);

    record TargetResult(UUID targetId, Map<String, Object> payload, long version) {}
}
