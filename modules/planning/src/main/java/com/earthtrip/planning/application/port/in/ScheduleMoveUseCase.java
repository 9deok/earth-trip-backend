package com.earthtrip.planning.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ScheduleMoveUseCase {

    MoveResult move(UUID tripId, UUID actorUserId, MoveCommand command);

    record MoveCommand(
            UUID itemId,
            UUID sourceDayId,
            UUID targetDayId,
            long itemBaseVersion,
            List<OrderItem> sourceOrder,
            List<OrderItem> targetOrder) {}

    record OrderItem(UUID itemId, int sortOrder, long baseVersion) {}

    record MoveResult(
            UUID movedItemId,
            UUID sourceDayId,
            UUID targetDayId,
            List<PlanningResourceUseCase.ResourceResult> sourceItems,
            List<PlanningResourceUseCase.ResourceResult> targetItems) {}
}
