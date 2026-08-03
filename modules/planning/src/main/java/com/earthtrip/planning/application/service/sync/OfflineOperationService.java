package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OfflineOperationService implements OfflineOperationUseCase {

    private final TripAccess access;
    private final PlanningResourceUseCase resources;
    private final OfflineOperationExecutor executor;
    private final ActivityOperationStorePort operations;
    private final SyncStateStorePort conflicts;
    private final Clock clock;

    OfflineOperationService(
        TripAccess access,
        PlanningResourceUseCase resources,
        OfflineOperationExecutor executor,
        ActivityOperationStorePort operations,
        SyncStateStorePort conflicts,
        Clock clock
    ) {
        this.access = access;
        this.resources = resources;
        this.executor = executor;
        this.operations = operations;
        this.conflicts = conflicts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BatchResult execute(
        UUID tripId,
        UUID actorUserId,
        List<OperationCommand> commands
    ) {
        access.requireEditor(tripId, actorUserId);
        if (commands == null || commands.isEmpty() || commands.size() > 100) {
            throw EarthTripException.badRequest(
                "INVALID_OPERATION_BATCH_SIZE", "한 번에 1~100개의 오프라인 작업을 보낼 수 있습니다."
            );
        }
        if (commands.stream().map(OperationCommand::operationId).distinct().count()
            != commands.size()) {
            throw EarthTripException.badRequest(
                "DUPLICATE_OPERATION_ID", "같은 묶음에 중복된 operationId가 있습니다."
            );
        }
        List<OperationResult> results = new ArrayList<>();
        for (OperationCommand command : commands) {
            results.add(process(tripId, actorUserId, command));
        }
        return new BatchResult(
            List.copyOf(results), count(results, "ACCEPTED"), count(results, "FAILED"),
            count(results, "CONFLICT")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OperationResult get(UUID tripId, UUID operationId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        ActivityOperationStorePort.OperationRecord record = operations
            .findOperation(operationId)
            .filter(item -> item.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "OPERATION_NOT_FOUND", "오프라인 작업 결과를 찾을 수 없습니다."
            ));
        return result(record);
    }

    private OperationResult process(
        UUID tripId,
        UUID actorUserId,
        OperationCommand command
    ) {
        if (command.operationId() == null) {
            return failedWithoutId(command, "OPERATION_ID_REQUIRED", "operationId가 필요합니다.");
        }
        ActivityOperationStorePort.OperationRecord existing = operations
            .findOperation(command.operationId())
            .orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId) || !existing.actorId().equals(actorUserId)) {
                throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "이미 다른 오프라인 작업에 사용된 operationId입니다."
                );
            }
            return result(existing);
        }
        Instant now = clock.instant();
        try {
            OfflineOperationExecutor.ExecutionResult applied = executor.execute(
                tripId, actorUserId, command, null, null
            );
            Map<String, Object> result = new LinkedHashMap<>(applied.result());
            return result(operations.saveOperation(new ActivityOperationStorePort.OperationRecord(
                command.operationId(), tripId, actorUserId, "ACCEPTED",
                command.resourceType(), applied.resourceId(), Map.copyOf(result), now
            )));
        } catch (EarthTripException exception) {
            if (exception.code().equals("VERSION_CONFLICT")) {
                return conflict(tripId, actorUserId, command, now, exception);
            }
            return failed(tripId, actorUserId, command, now, exception);
        }
    }

    private OperationResult conflict(
        UUID tripId,
        UUID actorUserId,
        OperationCommand command,
        Instant now,
        EarthTripException exception
    ) {
        Map<String, Object> serverSnapshot = serverSnapshot(tripId, actorUserId, command);
        UUID conflictId = UUID.nameUUIDFromBytes(
            ("earthtrip:sync-conflict:" + command.operationId())
                .getBytes(StandardCharsets.UTF_8)
        );
        List<String> mergeableFields = command.payload() == null
            ? List.of()
            : command.payload().keySet().stream().sorted().toList();
        conflicts.saveConflict(new SyncStateStorePort.ConflictRecord(
            conflictId, command.operationId(), tripId, actorUserId, command.action(),
            command.resourceType(), command.resourceId(),
            OfflineOperationCodec.command(command), serverSnapshot, mergeableFields,
            "OPEN", null, now, null, 0
        ));
        Map<String, Object> result = errorResult(exception.code(), exception.getMessage());
        result.put("conflictId", conflictId.toString());
        return result(operations.saveOperation(new ActivityOperationStorePort.OperationRecord(
            command.operationId(), tripId, actorUserId, "CONFLICT",
            command.resourceType(), command.resourceId(), Map.copyOf(result), now
        )));
    }

    private OperationResult failed(
        UUID tripId,
        UUID actorUserId,
        OperationCommand command,
        Instant now,
        EarthTripException exception
    ) {
        return result(operations.saveOperation(new ActivityOperationStorePort.OperationRecord(
            command.operationId(), tripId, actorUserId, "FAILED", command.resourceType(),
            command.resourceId(), errorResult(exception.code(), exception.getMessage()), now
        )));
    }

    private OperationResult failedWithoutId(
        OperationCommand command,
        String code,
        String message
    ) {
        return new OperationResult(
            null, "FAILED", command.resourceType(), command.resourceId(), null,
            Map.of(), code, message, clock.instant()
        );
    }

    private Map<String, Object> serverSnapshot(
        UUID tripId,
        UUID actorUserId,
        OperationCommand command
    ) {
        try {
            return OfflineOperationCodec.resource(resources.get(
                tripId, actorUserId, command.resourceType(), command.resourceId()
            ));
        } catch (EarthTripException exception) {
            if (exception.httpStatus() == 404) {
                return null;
            }
            throw exception;
        }
    }

    private static Map<String, Object> errorResult(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("errorCode", code);
        result.put("errorMessage", message);
        return result;
    }

    private static OperationResult result(ActivityOperationStorePort.OperationRecord record) {
        Map<String, Object> value = record.result();
        return new OperationResult(
            record.operationId(), record.status(), record.resourceType(), record.resourceId(),
            OfflineOperationCodec.uuid(value.get("conflictId")), value,
            text(value.get("errorCode")), text(value.get("errorMessage")), record.createdAt()
        );
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int count(List<OperationResult> results, String status) {
        return (int) results.stream().filter(item -> item.status().equals(status)).count();
    }
}
