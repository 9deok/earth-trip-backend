package com.earthtrip.trip.adapter.out.persistence.template;

import com.earthtrip.trip.application.port.out.TripTemplateStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripTemplatePersistenceAdapter implements TripTemplateStorePort {

    private static final TypeReference<Set<String>> SCOPES = new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> SNAPSHOT = new TypeReference<>() { };
    private final TripTemplateJpaRepository repository;
    private final TripTemplateDraftJpaRepository drafts;
    private final ObjectMapper json;

    TripTemplatePersistenceAdapter(
        TripTemplateJpaRepository repository,
        TripTemplateDraftJpaRepository drafts,
        ObjectMapper json
    ) {
        this.repository = repository;
        this.drafts = drafts;
        this.json = json;
    }

    @Override
    public List<TemplateRecord> findAll(UUID ownerUserId) {
        return repository
            .findAllByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ownerUserId.toString())
            .stream().map(this::record).toList();
    }

    @Override
    public Optional<TemplateRecord> find(UUID templateId) {
        return repository.findById(templateId.toString()).map(this::record);
    }

    @Override
    public TemplateRecord save(TemplateRecord template) {
        String scopes = write(template.includeScopes());
        String snapshot = write(template.snapshot());
        TripTemplateJpaEntity entity = repository.findById(template.id().toString())
            .map(existing -> {
                existing.apply(template, scopes, snapshot);
                return existing;
            })
            .orElseGet(() -> new TripTemplateJpaEntity(template, scopes, snapshot));
        return record(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<DraftRecord> findDraft(UUID requestId) {
        return drafts.findById(requestId.toString()).map(TripTemplateDraftJpaEntity::toRecord);
    }

    @Override
    public DraftRecord saveDraft(DraftRecord draft) {
        return drafts.save(new TripTemplateDraftJpaEntity(draft)).toRecord();
    }

    private TemplateRecord record(TripTemplateJpaEntity entity) {
        return entity.toRecord(
            read(entity.includeScopes(), SCOPES), read(entity.snapshot(), SNAPSHOT)
        );
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("여행 템플릿을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 여행 템플릿을 읽을 수 없습니다.", exception);
        }
    }
}
