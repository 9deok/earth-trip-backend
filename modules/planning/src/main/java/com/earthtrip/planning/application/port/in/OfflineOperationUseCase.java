package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OfflineOperationUseCase {

    BatchResult execute(UUID tripId, UUID actorUserId, List<OperationCommand> operations);

    OperationResult get(UUID tripId, UUID operationId, UUID actorUserId);

    record OperationCommand(
            UUID operationId,
            String action,
            String resourceType,
            UUID resourceId,
            UUID parentId,
            LocalDate localDate,
            Map<String, Object> payload,
            String status,
            Integer sortOrder,
            long baseVersion,
            String stateType,
            Map<String, Object> stateValue) {}

    record OperationResult(
            UUID operationId,
            String status,
            String resourceType,
            UUID resourceId,
            UUID conflictId,
            Map<String, Object> result,
            String errorCode,
            String errorMessage,
            Instant processedAt) {}

    record BatchResult(List<OperationResult> results, int accepted, int failed, int conflicted) {}
}
