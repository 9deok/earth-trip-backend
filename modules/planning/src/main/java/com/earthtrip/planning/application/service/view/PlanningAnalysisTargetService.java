package com.earthtrip.planning.application.service.view;

import com.earthtrip.planning.api.PlanningAnalysisTarget;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PlanningAnalysisTargetService implements PlanningAnalysisTarget {

    private final PlanningResourceUseCase resources;

    PlanningAnalysisTargetService(PlanningResourceUseCase resources) {
        this.resources = resources;
    }

    @Override
    @Transactional(readOnly = true)
    public TargetResult getResearchSource(UUID tripId, UUID sourceId, UUID actorUserId) {
        return result(resources.get(tripId, actorUserId, "RESEARCH_SOURCE", sourceId));
    }

    @Override
    public TargetResult confirmResearchSource(
        UUID tripId,
        UUID sourceId,
        UUID actorUserId,
        Map<String, Object> confirmedFields,
        long baseVersion
    ) {
        PlanningResourceUseCase.ResourceResult current = resources.get(
            tripId, actorUserId, "RESEARCH_SOURCE", sourceId
        );
        Map<String, Object> payload = new LinkedHashMap<>(current.payload());
        if (confirmedFields != null) {
            payload.putAll(confirmedFields);
        }
        return result(resources.update(
            tripId, actorUserId, "RESEARCH_SOURCE", sourceId,
            PlanningResourceUseCase.WritePermission.EDITOR,
            new PlanningResourceUseCase.ResourceCommand(
                sourceId, null, null, Map.copyOf(payload), current.status(),
                current.sortOrder(), baseVersion
            )
        ));
    }

    private static TargetResult result(PlanningResourceUseCase.ResourceResult resource) {
        return new TargetResult(resource.resourceId(), resource.payload(), resource.version());
    }
}
