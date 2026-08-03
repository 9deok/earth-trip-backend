package com.earthtrip.planning.adapter.out.persistence.merge;

import com.earthtrip.planning.application.port.out.PlanningMergeStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PlanningMergePersistenceAdapter implements PlanningMergeStorePort {

    private static final TypeReference<List<UUID>> UUIDS = new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<List<Map<String, Object>>> LINKS = new TypeReference<>() { };

    private final PlanningMergeJpaRepository repository;
    private final ObjectMapper json;

    PlanningMergePersistenceAdapter(PlanningMergeJpaRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public Optional<MergeRecord> find(UUID mergeId) {
        return repository.findById(mergeId.toString()).map(this::record);
    }

    @Override
    public MergeRecord save(MergeRecord merge) {
        PlanningMergeJpaEntity.SerializedMerge serialized = serialize(merge);
        PlanningMergeJpaEntity entity = repository.findById(merge.id().toString())
            .map(existing -> {
                existing.apply(merge, serialized);
                return existing;
            })
            .orElseGet(() -> new PlanningMergeJpaEntity(merge, serialized));
        return record(repository.saveAndFlush(entity));
    }

    private MergeRecord record(PlanningMergeJpaEntity entity) {
        return entity.toRecord(
            read(entity.duplicateIds(), UUIDS),
            read(entity.beforeSnapshot(), MAP),
            read(entity.afterSnapshot(), MAP),
            read(entity.addedLinks(), LINKS)
        );
    }

    private PlanningMergeJpaEntity.SerializedMerge serialize(MergeRecord merge) {
        return new PlanningMergeJpaEntity.SerializedMerge(
            write(merge.duplicateIds()), write(merge.beforeSnapshot()),
            write(merge.afterSnapshot()), write(merge.addedLinks())
        );
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("병합 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 병합 스냅샷을 읽을 수 없습니다.", exception);
        }
    }
}
