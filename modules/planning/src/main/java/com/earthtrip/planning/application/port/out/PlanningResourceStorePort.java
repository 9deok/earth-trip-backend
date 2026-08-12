package com.earthtrip.planning.application.port.out;

import com.earthtrip.planning.domain.PlanningResource;
import java.time.*;
import java.util.*;

public interface PlanningResourceStorePort {
    List<PlanningResource> findAll(UUID tripId);

    List<PlanningResource> findAll(UUID tripId, String type, UUID parentId, LocalDate localDate);

    Optional<PlanningResource> findById(UUID id);

    PlanningResource save(PlanningResource r);

    void delete(UUID id);

    List<UserStateRecord> userStates(UUID resourceId);

    UserStateRecord saveUserState(
            UUID resourceId, UUID userId, String type, Map<String, Object> value, Instant now);

    void deleteUserState(UUID resourceId, UUID userId, String type);

    default Map<UUID, List<UserStateRecord>> userStates(Collection<UUID> resourceIds) {
        Map<UUID, List<UserStateRecord>> result = new LinkedHashMap<>();
        for (UUID resourceId : resourceIds) result.put(resourceId, userStates(resourceId));
        return result;
    }

    void appendActivity(
            UUID tripId,
            UUID actor,
            String action,
            String resourceType,
            UUID resourceId,
            Map<String, Object> payload,
            Instant now);

    record UserStateRecord(
            UUID userId, String stateType, Map<String, Object> value, Instant updatedAt) {}
}
