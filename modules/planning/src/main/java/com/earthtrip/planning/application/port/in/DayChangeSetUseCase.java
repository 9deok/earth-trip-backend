package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DayChangeSetUseCase {

    ChangeSetResult apply(
        UUID tripId,
        UUID dayId,
        UUID actorUserId,
        ChangeSetCommand command
    );

    ChangeSetResult revert(
        UUID tripId,
        UUID dayId,
        UUID changeSetId,
        UUID actorUserId,
        long baseVersion
    );

    record ChangeSetCommand(UUID requestId, List<OrderItem> order) { }

    record OrderItem(UUID itemId, int sortOrder, long baseVersion) { }

    record ChangeSetResult(
        UUID changeSetId,
        UUID tripId,
        UUID dayId,
        String status,
        List<UUID> previousOrder,
        List<UUID> appliedOrder,
        Instant appliedAt,
        Instant revertedAt,
        long version
    ) { }
}
