package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.in.SyncConflictUseCase;
import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SyncConflictService implements SyncConflictUseCase {

    private final TripAccess access;
    private final PlanningResourceUseCase resources;
    private final OfflineOperationExecutor executor;
    private final SyncStateStorePort conflicts;
    private final ActivityOperationStorePort operations;
    private final Clock clock;

    SyncConflictService(
            TripAccess access,
            PlanningResourceUseCase resources,
            OfflineOperationExecutor executor,
            SyncStateStorePort conflicts,
            ActivityOperationStorePort operations,
            Clock clock) {
        this.access = access;
        this.resources = resources;
        this.executor = executor;
        this.conflicts = conflicts;
        this.operations = operations;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return conflicts.findOpenConflicts(tripId).stream().map(this::result).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictResult get(UUID tripId, UUID conflictId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return result(load(tripId, conflictId));
    }

    @Override
    public ConflictResult resolve(
            UUID tripId, UUID conflictId, UUID actorUserId, ResolutionCommand command) {
        access.requireEditor(tripId, actorUserId);
        SyncStateStorePort.ConflictRecord conflict = load(tripId, conflictId);
        if (!conflict.status().equals("OPEN")) {
            return result(conflict);
        }
        if (conflict.version() != command.baseVersion()) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 사용자가 충돌을 먼저 해결했습니다.",
                    Map.of("serverVersion", conflict.version()));
        }
        String strategy = strategy(command.strategy());
        Map<String, Object> applied = Map.of();
        if (!strategy.equals("SERVER")) {
            OfflineOperationUseCase.OperationCommand operation =
                    OfflineOperationCodec.command(conflict.deviceCommand());
            PlanningResourceUseCase.ResourceResult current =
                    resources.get(
                            tripId, actorUserId, conflict.resourceType(), conflict.resourceId());
            Map<String, Object> payload =
                    strategy.equals("MERGED")
                            ? mergedPayload(
                                    current.payload(),
                                    conflict.mergeableFields(),
                                    command.mergedPayload())
                            : operation.payload();
            OfflineOperationExecutor.ExecutionResult execution =
                    executor.execute(tripId, actorUserId, operation, current.version(), payload);
            applied = execution.result();
        }
        Instant now = clock.instant();
        SyncStateStorePort.ConflictRecord saved =
                conflicts.saveConflict(
                        new SyncStateStorePort.ConflictRecord(
                                conflict.conflictId(),
                                conflict.operationId(),
                                tripId,
                                conflict.actorId(),
                                conflict.action(),
                                conflict.resourceType(),
                                conflict.resourceId(),
                                conflict.deviceCommand(),
                                conflict.serverSnapshot(),
                                conflict.mergeableFields(),
                                "RESOLVED",
                                strategy,
                                conflict.createdAt(),
                                now,
                                conflict.version()));
        Map<String, Object> operationResult = new LinkedHashMap<>(applied);
        operationResult.put("resolvedConflictId", conflictId.toString());
        operationResult.put("resolution", strategy);
        operations.saveOperation(
                new ActivityOperationStorePort.OperationRecord(
                        conflict.operationId(),
                        tripId,
                        conflict.actorId(),
                        "ACCEPTED",
                        conflict.resourceType(),
                        conflict.resourceId(),
                        Map.copyOf(operationResult),
                        now));
        return result(saved);
    }

    private SyncStateStorePort.ConflictRecord load(UUID tripId, UUID conflictId) {
        return conflicts
                .findConflict(conflictId)
                .filter(item -> item.tripId().equals(tripId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "SYNC_CONFLICT_NOT_FOUND", "동기화 충돌을 찾을 수 없습니다."));
    }

    private static String strategy(String value) {
        if (value == null) {
            throw EarthTripException.badRequest(
                    "CONFLICT_RESOLUTION_REQUIRED", "충돌 해결 방식을 선택해 주세요.");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!List.of("SERVER", "DEVICE", "MERGED").contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_CONFLICT_RESOLUTION", "SERVER, DEVICE, MERGED 중 하나를 선택해 주세요.");
        }
        return normalized;
    }

    private static Map<String, Object> mergedPayload(
            Map<String, Object> serverPayload,
            List<String> mergeableFields,
            Map<String, Object> requested) {
        if (requested == null || requested.isEmpty()) {
            throw EarthTripException.badRequest("MERGED_PAYLOAD_REQUIRED", "필드 병합에는 병합할 값이 필요합니다.");
        }
        if (!mergeableFields.containsAll(requested.keySet())) {
            throw EarthTripException.badRequest(
                    "FIELD_NOT_MERGEABLE", "자동 병합할 수 없는 필드가 포함되어 있습니다.");
        }
        Map<String, Object> merged = new LinkedHashMap<>(serverPayload);
        merged.putAll(requested);
        return Map.copyOf(merged);
    }

    private ConflictResult result(SyncStateStorePort.ConflictRecord record) {
        return new ConflictResult(
                record.conflictId(),
                record.operationId(),
                record.action(),
                record.resourceType(),
                record.resourceId(),
                record.deviceCommand(),
                record.serverSnapshot(),
                record.mergeableFields(),
                record.status(),
                record.resolution(),
                record.createdAt(),
                record.resolvedAt(),
                record.version());
    }
}
