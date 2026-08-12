package com.earthtrip.platform.adapter.out.persistence.export;

import com.earthtrip.platform.application.port.out.TripExportStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripExportPersistenceAdapter implements TripExportStorePort {

    private static final TypeReference<Set<String>> SCOPES = new TypeReference<>() {};
    private final TripExportJpaRepository repository;
    private final ObjectMapper json;

    TripExportPersistenceAdapter(TripExportJpaRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public Optional<ExportRecord> find(UUID exportId) {
        return repository.findById(exportId.toString()).map(this::record);
    }

    @Override
    public ExportRecord save(ExportRecord export) {
        String scopes = write(export.scopes());
        TripExportJpaEntity entity =
                repository
                        .findById(export.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(export, scopes);
                                    return existing;
                                })
                        .orElseGet(() -> new TripExportJpaEntity(export, scopes));
        return record(repository.saveAndFlush(entity));
    }

    private ExportRecord record(TripExportJpaEntity entity) {
        return entity.toRecord(read(entity.scopes()));
    }

    private String write(Set<String> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("여행 내보내기 범위를 저장할 수 없습니다.", exception);
        }
    }

    private Set<String> read(String value) {
        try {
            return json.readValue(value, SCOPES);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 여행 내보내기 범위를 읽을 수 없습니다.", exception);
        }
    }
}
