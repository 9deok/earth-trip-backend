package com.earthtrip.planning.adapter.out.persistence.resource;

import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class ActivityOperationPersistenceAdapter implements ActivityOperationStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final PlanningActivityJpaRepository activities;
    private final PlanningOperationResultJpaRepository operations;
    private final ObjectMapper json;

    ActivityOperationPersistenceAdapter(
        PlanningActivityJpaRepository activities,
        PlanningOperationResultJpaRepository operations,
        ObjectMapper json
    ) {
        this.activities = activities;
        this.operations = operations;
        this.json = json;
    }

    @Override
    public List<ActivityRecord> activities(UUID tripId, long after, int limit) {
        return activities.findAllByTripIdAndSequenceIdGreaterThanOrderBySequenceIdAsc(
            tripId.toString(), after, PageRequest.of(0, limit)
        ).stream().map(this::activity).toList();
    }

    @Override
    public Optional<ActivityRecord> findActivity(UUID activityId) {
        return activities.findByEventId(activityId.toString()).map(this::activity);
    }

    @Override
    public long latestSequence(UUID tripId) {
        return activities.findFirstByTripIdOrderBySequenceIdDesc(tripId.toString())
            .map(PlanningActivityJpaEntity::sequenceId)
            .orElse(0L);
    }

    @Override
    public Optional<OperationRecord> findOperation(UUID operationId) {
        return operations.findById(operationId.toString()).map(this::operation);
    }

    @Override
    public OperationRecord saveOperation(OperationRecord record) {
        return operation(operations.save(new PlanningOperationResultJpaEntity(
            record, write(record.result())
        )));
    }

    private ActivityRecord activity(PlanningActivityJpaEntity entity) {
        return new ActivityRecord(
            entity.sequenceId(), entity.eventId(), entity.tripId(), entity.actorId(),
            entity.action(), entity.resourceType(), entity.resourceId(),
            read(entity.payload()), entity.occurredAt()
        );
    }

    private OperationRecord operation(PlanningOperationResultJpaEntity entity) {
        return new OperationRecord(
            entity.operationId(), entity.tripId(), entity.actorId(), entity.status(),
            entity.resourceType(), entity.resourceId(), read(entity.resultJson()),
            entity.createdAt()
        );
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("작업 결과를 JSON으로 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 동기화 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
