package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RouteOverrideUseCase {

    List<OverrideResult> list(UUID tripId, UUID dayId, UUID actorUserId);

    OverrideResult create(UUID tripId, UUID dayId, UUID actorUserId, OverrideCommand command);

    void delete(UUID tripId, UUID dayId, UUID overrideId, UUID actorUserId, long baseVersion);

    record OverrideCommand(
            UUID requestId,
            UUID fromItemId,
            UUID toItemId,
            Integer durationMinutes,
            Long distanceMeters,
            String mode,
            String note) {}

    record OverrideResult(
            UUID overrideId,
            UUID fromItemId,
            UUID toItemId,
            int durationMinutes,
            Long distanceMeters,
            String mode,
            String note,
            long version,
            UUID updatedBy,
            Instant updatedAt) {}
}
