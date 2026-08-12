package com.earthtrip.planning.api;

import java.util.Map;
import java.util.UUID;

public interface PlanningComparisonTarget {
    Target get(UUID tripId, UUID optionId, UUID actorUserId);

    Target applyRefresh(
            UUID tripId,
            UUID optionId,
            UUID actorUserId,
            Map<String, Object> fields,
            long baseVersion);

    record Target(UUID optionId, Map<String, Object> payload, String status, long version) {}
}
