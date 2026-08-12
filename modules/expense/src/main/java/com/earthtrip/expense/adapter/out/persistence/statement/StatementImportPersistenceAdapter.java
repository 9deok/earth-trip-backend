package com.earthtrip.expense.adapter.out.persistence.statement;

import com.earthtrip.expense.application.port.out.StatementImportStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class StatementImportPersistenceAdapter implements StatementImportStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final StatementImportJpaRepository imports;
    private final StatementImportCandidateJpaRepository candidates;
    private final ObjectMapper json;

    StatementImportPersistenceAdapter(
            StatementImportJpaRepository imports,
            StatementImportCandidateJpaRepository candidates,
            ObjectMapper json) {
        this.imports = imports;
        this.candidates = candidates;
        this.json = json;
    }

    @Override
    public List<ImportRecord> findImports(UUID tripId) {
        return imports.findAllByTripIdOrderByCreatedAtDesc(tripId.toString()).stream()
                .map(StatementImportJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<ImportRecord> findImport(UUID importId) {
        return imports.findById(importId.toString()).map(StatementImportJpaEntity::toRecord);
    }

    @Override
    public ImportRecord saveImport(ImportRecord record) {
        StatementImportJpaEntity entity =
                imports.findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record);
                                    return existing;
                                })
                        .orElseGet(() -> new StatementImportJpaEntity(record));
        return imports.saveAndFlush(entity).toRecord();
    }

    @Override
    public List<CandidateRecord> findCandidates(UUID importId) {
        return candidates.findAllByImportIdOrderByOccurredAtAsc(importId.toString()).stream()
                .map(this::candidate)
                .toList();
    }

    @Override
    public Optional<CandidateRecord> findCandidate(UUID candidateId) {
        return candidates.findById(candidateId.toString()).map(this::candidate);
    }

    @Override
    public CandidateRecord saveCandidate(CandidateRecord record) {
        String payload = write(record.payload());
        StatementImportCandidateJpaEntity entity =
                candidates
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, payload);
                                    return existing;
                                })
                        .orElseGet(() -> new StatementImportCandidateJpaEntity(record, payload));
        return candidate(candidates.saveAndFlush(entity));
    }

    private CandidateRecord candidate(StatementImportCandidateJpaEntity entity) {
        return entity.toRecord(read(entity.payload()));
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("카드 내역 후보 JSON을 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 카드 내역 후보를 읽을 수 없습니다.", exception);
        }
    }
}
