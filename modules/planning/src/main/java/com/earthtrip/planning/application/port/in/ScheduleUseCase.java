package com.earthtrip.planning.application.port.in;

import java.util.*;

public interface ScheduleUseCase {
    List<PlanningResourceUseCase.ResourceResult> list(UUID tripId, UUID dayId, UUID actor);

    PlanningResourceUseCase.ResourceResult get(UUID tripId, UUID dayId, UUID itemId, UUID actor);

    PlanningResourceUseCase.ResourceResult create(
            UUID tripId, UUID dayId, UUID actor, PlanningResourceUseCase.ResourceCommand command);

    PlanningResourceUseCase.ResourceResult update(
            UUID tripId,
            UUID dayId,
            UUID itemId,
            UUID actor,
            PlanningResourceUseCase.ResourceCommand command);

    void delete(UUID tripId, UUID dayId, UUID itemId, UUID actor, long baseVersion);

    List<PlanningResourceUseCase.ResourceResult> reorder(
            UUID tripId, UUID dayId, UUID actor, List<OrderItem> items);

    record OrderItem(UUID itemId, int sortOrder, long baseVersion) {}
}
