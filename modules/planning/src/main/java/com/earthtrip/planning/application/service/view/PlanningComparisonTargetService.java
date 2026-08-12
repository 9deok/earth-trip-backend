package com.earthtrip.planning.application.service.view;

import com.earthtrip.planning.api.PlanningComparisonTarget;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PlanningComparisonTargetService implements PlanningComparisonTarget {
    private final PlanningResourceUseCase resources;

    PlanningComparisonTargetService(PlanningResourceUseCase r) {
        resources = r;
    }

    @Override
    @Transactional(readOnly = true)
    public Target get(UUID trip, UUID id, UUID actor) {
        return result(resources.get(trip, actor, "COMPARISON_OPTION", id));
    }

    @Override
    public Target applyRefresh(
            UUID trip, UUID id, UUID actor, Map<String, Object> fields, long version) {
        PlanningResourceUseCase.ResourceResult current =
                resources.get(trip, actor, "COMPARISON_OPTION", id);
        Map<String, Object> payload = new LinkedHashMap<>(current.payload());
        payload.putAll(fields);
        return result(
                resources.update(
                        trip,
                        actor,
                        "COMPARISON_OPTION",
                        id,
                        PlanningResourceUseCase.WritePermission.EDITOR,
                        new PlanningResourceUseCase.ResourceCommand(
                                id,
                                null,
                                null,
                                Map.copyOf(payload),
                                current.status(),
                                current.sortOrder(),
                                version)));
    }

    private static Target result(PlanningResourceUseCase.ResourceResult r) {
        return new Target(r.resourceId(), r.payload(), r.status(), r.version());
    }
}
