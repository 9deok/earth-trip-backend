package com.earthtrip.planning.application.service.sync;

import com.earthtrip.planning.application.port.in.OfflineOperationUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OfflineOperationExecutor {

    private final PlanningResourceUseCase resources;

    OfflineOperationExecutor(PlanningResourceUseCase resources) {
        this.resources = resources;
    }

    @Transactional
    ExecutionResult execute(
            UUID tripId,
            UUID actorUserId,
            OfflineOperationUseCase.OperationCommand command,
            Long forcedBaseVersion,
            Map<String, Object> forcedPayload) {
        requireCommand(command);
        String action = command.action().strip().toUpperCase(Locale.ROOT);
        long baseVersion = forcedBaseVersion == null ? command.baseVersion() : forcedBaseVersion;
        Map<String, Object> payload = forcedPayload == null ? command.payload() : forcedPayload;
        PlanningResourceUseCase.ResourceResult resource;
        switch (action) {
            case "CREATE" ->
                    resource =
                            resources.create(
                                    tripId,
                                    actorUserId,
                                    command.resourceType(),
                                    PlanningResourceUseCase.WritePermission.EDITOR,
                                    new PlanningResourceUseCase.ResourceCommand(
                                            command.resourceId(),
                                            command.parentId(),
                                            command.localDate(),
                                            payload,
                                            command.status(),
                                            command.sortOrder(),
                                            0));
            case "UPDATE" ->
                    resource =
                            resources.update(
                                    tripId,
                                    actorUserId,
                                    command.resourceType(),
                                    command.resourceId(),
                                    PlanningResourceUseCase.WritePermission.EDITOR,
                                    new PlanningResourceUseCase.ResourceCommand(
                                            command.resourceId(),
                                            command.parentId(),
                                            command.localDate(),
                                            payload,
                                            command.status(),
                                            command.sortOrder(),
                                            baseVersion));
            case "RELOCATE" ->
                    resource =
                            resources.relocate(
                                    tripId,
                                    actorUserId,
                                    command.resourceType(),
                                    command.resourceId(),
                                    PlanningResourceUseCase.WritePermission.EDITOR,
                                    command.parentId(),
                                    command.localDate(),
                                    requiredSortOrder(command.sortOrder()),
                                    baseVersion);
            case "DELETE" -> {
                resources.delete(
                        tripId,
                        actorUserId,
                        command.resourceType(),
                        command.resourceId(),
                        PlanningResourceUseCase.WritePermission.EDITOR,
                        baseVersion);
                return new ExecutionResult(command.resourceId(), Map.of("deleted", true));
            }
            case "PUT_USER_STATE" -> {
                PlanningResourceUseCase.UserStateResult state =
                        resources.putUserState(
                                tripId,
                                actorUserId,
                                command.resourceType(),
                                command.resourceId(),
                                requireStateType(command.stateType()),
                                requiredStateValue(command.stateValue()));
                return new ExecutionResult(
                        command.resourceId(),
                        Map.of(
                                "stateType", state.stateType(),
                                "updatedAt", state.updatedAt().toString()));
            }
            case "DELETE_USER_STATE" -> {
                resources.deleteUserState(
                        tripId,
                        actorUserId,
                        command.resourceType(),
                        command.resourceId(),
                        requireStateType(command.stateType()));
                return new ExecutionResult(command.resourceId(), Map.of("deleted", true));
            }
            default ->
                    throw EarthTripException.badRequest(
                            "INVALID_OFFLINE_OPERATION_ACTION", "지원하지 않는 오프라인 작업 유형입니다.");
        }
        return new ExecutionResult(resource.resourceId(), OfflineOperationCodec.resource(resource));
    }

    private static void requireCommand(OfflineOperationUseCase.OperationCommand command) {
        if (command.operationId() == null
                || command.action() == null
                || command.action().isBlank()
                || command.resourceType() == null
                || command.resourceType().isBlank()
                || command.resourceId() == null) {
            throw EarthTripException.badRequest(
                    "INVALID_OFFLINE_OPERATION", "작업 ID, 유형, 리소스 유형과 ID가 필요합니다.");
        }
    }

    private static int requiredSortOrder(Integer value) {
        if (value == null || value < 0) {
            throw EarthTripException.badRequest(
                    "SORT_ORDER_REQUIRED", "이동 작업에는 0 이상의 정렬 순서가 필요합니다.");
        }
        return value;
    }

    private static String requireStateType(String value) {
        if (value == null || value.isBlank()) {
            throw EarthTripException.badRequest("STATE_TYPE_REQUIRED", "사용자 상태 유형이 필요합니다.");
        }
        return value;
    }

    private static Map<String, Object> requiredStateValue(Map<String, Object> value) {
        if (value == null) {
            throw EarthTripException.badRequest("STATE_VALUE_REQUIRED", "사용자 상태 값이 필요합니다.");
        }
        return value;
    }

    record ExecutionResult(UUID resourceId, Map<String, Object> result) {}
}
