package com.earthtrip.planning.application.port.in;

import java.time.*;
import java.util.*;

public interface PlanningResourceUseCase {
    enum WritePermission {
        MEMBER,
        EDITOR,
        OWNER
    }

    List<ResourceResult> listAll(UUID tripId, UUID actor);

    List<ResourceResult> list(
            UUID tripId, UUID actor, String type, UUID parentId, LocalDate localDate);

    ResourceResult get(UUID tripId, UUID actor, String type, UUID resourceId);

    ResourceResult create(
            UUID tripId,
            UUID actor,
            String type,
            WritePermission permission,
            ResourceCommand command);

    ResourceResult update(
            UUID tripId,
            UUID actor,
            String type,
            UUID resourceId,
            WritePermission permission,
            ResourceCommand command);

    ResourceResult relocate(
            UUID tripId,
            UUID actor,
            String type,
            UUID resourceId,
            WritePermission permission,
            UUID parentId,
            LocalDate localDate,
            int sortOrder,
            long baseVersion);

    void delete(
            UUID tripId,
            UUID actor,
            String type,
            UUID resourceId,
            WritePermission permission,
            long baseVersion);

    UserStateResult putUserState(
            UUID tripId,
            UUID actor,
            String type,
            UUID resourceId,
            String stateType,
            Map<String, Object> value);

    void deleteUserState(UUID tripId, UUID actor, String type, UUID resourceId, String stateType);

    record ResourceCommand(
            UUID requestId,
            UUID parentId,
            LocalDate localDate,
            Map<String, Object> payload,
            String status,
            Integer sortOrder,
            long baseVersion) {}

    record UserStateResult(
            UUID userId, String stateType, Map<String, Object> value, Instant updatedAt) {}

    record ResourceResult(
            UUID resourceId,
            UUID tripId,
            String resourceType,
            UUID parentId,
            LocalDate localDate,
            Map<String, Object> payload,
            String status,
            int sortOrder,
            List<UserStateResult> userStates,
            long version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {}
}
