package com.earthtrip.wallet.adapter.out.persistence.template;

import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PackingTemplatePersistenceAdapter implements PackingTemplateStorePort {

    private static final TypeReference<List<PackingTemplateUseCase.TemplateItem>> ITEMS =
            new TypeReference<>() {};
    private static final TypeReference<List<UUID>> IDS = new TypeReference<>() {};

    private final PackingTemplateJpaRepository templates;
    private final PackingTemplateApplicationJpaRepository applications;
    private final PreparationSuggestionDismissalJpaRepository dismissals;
    private final ObjectMapper json;

    PackingTemplatePersistenceAdapter(
            PackingTemplateJpaRepository templates,
            PackingTemplateApplicationJpaRepository applications,
            PreparationSuggestionDismissalJpaRepository dismissals,
            ObjectMapper json) {
        this.templates = templates;
        this.applications = applications;
        this.dismissals = dismissals;
        this.json = json;
    }

    @Override
    public List<TemplateRecord> findVisible(UUID userId) {
        return templates.findVisible(userId.toString()).stream().map(this::template).toList();
    }

    @Override
    public Optional<TemplateRecord> findById(UUID templateId) {
        return templates
                .findById(templateId.toString())
                .map(this::template)
                .filter(record -> record.deletedAt() == null);
    }

    @Override
    public TemplateRecord save(TemplateRecord record) {
        String items = write(record.items());
        PackingTemplateJpaEntity entity =
                templates
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, items);
                                    return existing;
                                })
                        .orElseGet(() -> new PackingTemplateJpaEntity(record, items));
        return template(templates.saveAndFlush(entity));
    }

    @Override
    public Optional<ApplicationRecord> findApplication(UUID applicationId) {
        return applications.findById(applicationId.toString()).map(this::application);
    }

    @Override
    public ApplicationRecord saveApplication(ApplicationRecord record) {
        return application(
                applications.save(
                        new PackingTemplateApplicationJpaEntity(record, write(record.itemIds()))));
    }

    @Override
    public boolean isDismissed(UUID tripId, UUID userId, UUID suggestionId) {
        return dismissals.existsById(
                new PreparationSuggestionDismissalId(suggestionId.toString(), userId.toString()));
    }

    @Override
    public void saveDismissal(DismissalRecord record) {
        dismissals.save(new PreparationSuggestionDismissalJpaEntity(record));
    }

    private TemplateRecord template(PackingTemplateJpaEntity entity) {
        return new TemplateRecord(
                entity.id(),
                entity.userId(),
                entity.name(),
                entity.visibility(),
                read(entity.items(), ITEMS),
                entity.createdAt(),
                entity.updatedAt(),
                entity.deletedAt(),
                entity.version());
    }

    private ApplicationRecord application(PackingTemplateApplicationJpaEntity entity) {
        return new ApplicationRecord(
                entity.id(),
                entity.tripId(),
                entity.templateId(),
                entity.appliedBy(),
                entity.appliedAt(),
                read(entity.itemIds(), IDS));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("짐 템플릿 JSON을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 짐 템플릿 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
