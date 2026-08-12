package com.earthtrip.planning.adapter.out.persistence.resource;

import com.earthtrip.planning.application.port.out.PlanningResourceStorePort;
import com.earthtrip.planning.domain.PlanningResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class PlanningResourcePersistenceAdapter implements PlanningResourceStorePort {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final PlanningResourceJpaRepository resources;
    private final PlanningUserStateJpaRepository states;
    private final PlanningActivityJpaRepository activities;
    private final ObjectMapper json;

    PlanningResourcePersistenceAdapter(
            PlanningResourceJpaRepository r,
            PlanningUserStateJpaRepository s,
            PlanningActivityJpaRepository a,
            ObjectMapper j) {
        resources = r;
        states = s;
        activities = a;
        json = j;
    }

    @Override
    public List<PlanningResource> findAll(UUID trip) {
        return resources
                .findAllByTripIdAndDeletedAtIsNullOrderByResourceTypeAscSortOrderAscCreatedAtAsc(
                        trip.toString())
                .stream()
                .map(this::domain)
                .toList();
    }

    @Override
    public List<PlanningResource> findAll(UUID trip, String type, UUID parent, LocalDate date) {
        List<PlanningResourceJpaEntity> rows =
                parent != null
                        ? resources
                                .findAllByTripIdAndResourceTypeAndParentIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(
                                        trip.toString(), type, parent.toString())
                        : date != null
                                ? resources
                                        .findAllByTripIdAndResourceTypeAndLocalDateAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(
                                                trip.toString(), type, date)
                                : resources
                                        .findAllByTripIdAndResourceTypeAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(
                                                trip.toString(), type);
        return rows.stream().map(this::domain).toList();
    }

    @Override
    public Optional<PlanningResource> findById(UUID id) {
        return resources
                .findById(id.toString())
                .map(this::domain)
                .filter(r -> r.deletedAt() == null);
    }

    @Override
    public PlanningResource save(PlanningResource r) {
        String payload = write(r.payload());
        PlanningResourceJpaEntity e =
                resources
                        .findById(r.id().toString())
                        .map(
                                x -> {
                                    x.apply(r, payload);
                                    return x;
                                })
                        .orElseGet(() -> new PlanningResourceJpaEntity(r, payload));
        return domain(resources.saveAndFlush(e));
    }

    @Override
    public void delete(UUID id) {
        resources.deleteById(id.toString());
    }

    @Override
    public List<UserStateRecord> userStates(UUID id) {
        return states.findAllByResourceId(id.toString()).stream()
                .map(
                        s ->
                                new UserStateRecord(
                                        s.userId(),
                                        s.stateType(),
                                        read(s.valueJson()),
                                        s.updatedAt()))
                .toList();
    }

    @Override
    public Map<UUID, List<UserStateRecord>> userStates(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, List<UserStateRecord>> result = new LinkedHashMap<>();
        for (UUID id : ids) result.put(id, new ArrayList<>());
        for (PlanningUserStateJpaEntity state :
                states.findAllByResourceIdIn(ids.stream().map(UUID::toString).toList()))
            result.computeIfAbsent(state.resourceId(), ignored -> new ArrayList<>())
                    .add(
                            new UserStateRecord(
                                    state.userId(),
                                    state.stateType(),
                                    read(state.valueJson()),
                                    state.updatedAt()));
        return result.entrySet().stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> List.copyOf(entry.getValue()),
                                (left, right) -> left,
                                LinkedHashMap::new));
    }

    @Override
    public UserStateRecord saveUserState(
            UUID id, UUID user, String type, Map<String, Object> value, Instant now) {
        PlanningUserStateId key = new PlanningUserStateId(id.toString(), user.toString(), type);
        String data = write(value);
        PlanningUserStateJpaEntity e =
                states.findById(key)
                        .map(
                                x -> {
                                    x.apply(data, now);
                                    return x;
                                })
                        .orElseGet(
                                () ->
                                        new PlanningUserStateJpaEntity(
                                                id.toString(), user.toString(), type, data, now));
        PlanningUserStateJpaEntity saved = states.save(e);
        return new UserStateRecord(
                saved.userId(), saved.stateType(), read(saved.valueJson()), saved.updatedAt());
    }

    @Override
    public void deleteUserState(UUID id, UUID user, String type) {
        states.deleteById(new PlanningUserStateId(id.toString(), user.toString(), type));
    }

    @Override
    public void appendActivity(
            UUID trip,
            UUID actor,
            String action,
            String type,
            UUID id,
            Map<String, Object> payload,
            Instant now) {
        activities.save(
                new PlanningActivityJpaEntity(trip, actor, action, type, id, write(payload), now));
    }

    private PlanningResource domain(PlanningResourceJpaEntity e) {
        return e.toDomain(read(e.payload()));
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON으로 저장할 수 없는 값입니다.", e);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 JSON을 읽을 수 없습니다.", e);
        }
    }
}
