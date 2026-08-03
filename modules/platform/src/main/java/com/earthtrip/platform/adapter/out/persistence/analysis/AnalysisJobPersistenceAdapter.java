package com.earthtrip.platform.adapter.out.persistence.analysis;

import com.earthtrip.platform.application.port.out.AnalysisJobStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AnalysisJobPersistenceAdapter implements AnalysisJobStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<List<Map<String, Object>>> LIST = new TypeReference<>() { };
    private final AnalysisJobJpaRepository repository;
    private final ObjectMapper json;

    AnalysisJobPersistenceAdapter(AnalysisJobJpaRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public Optional<JobRecord> find(UUID jobId) {
        return repository.findById(jobId.toString()).map(this::record);
    }

    @Override
    public JobRecord save(JobRecord job) {
        AnalysisJobJpaEntity.Serialized serialized = new AnalysisJobJpaEntity.Serialized(
            write(job.inputPayload()), write(job.suggestions()),
            job.confirmedPayload() == null ? null : write(job.confirmedPayload())
        );
        AnalysisJobJpaEntity entity = repository.findById(job.id().toString())
            .map(existing -> {
                existing.apply(job, serialized);
                return existing;
            })
            .orElseGet(() -> new AnalysisJobJpaEntity(job, serialized));
        return record(repository.saveAndFlush(entity));
    }

    private JobRecord record(AnalysisJobJpaEntity entity) {
        return entity.toRecord(
            read(entity.inputPayload(), MAP), read(entity.suggestions(), LIST),
            entity.confirmedPayload() == null ? null : read(entity.confirmedPayload(), MAP)
        );
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("분석 작업 JSON을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 분석 작업 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
