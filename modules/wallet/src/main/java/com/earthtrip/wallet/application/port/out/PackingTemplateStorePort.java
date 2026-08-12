package com.earthtrip.wallet.application.port.out;

import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackingTemplateStorePort {

    List<TemplateRecord> findVisible(UUID userId);

    Optional<TemplateRecord> findById(UUID templateId);

    TemplateRecord save(TemplateRecord record);

    Optional<ApplicationRecord> findApplication(UUID applicationId);

    ApplicationRecord saveApplication(ApplicationRecord record);

    boolean isDismissed(UUID tripId, UUID userId, UUID suggestionId);

    void saveDismissal(DismissalRecord record);

    record TemplateRecord(
            UUID id,
            UUID userId,
            String name,
            String visibility,
            List<PackingTemplateUseCase.TemplateItem> items,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long version) {}

    record ApplicationRecord(
            UUID id,
            UUID tripId,
            UUID templateId,
            UUID appliedBy,
            Instant appliedAt,
            List<UUID> itemIds) {}

    record DismissalRecord(
            UUID suggestionId, UUID tripId, UUID userId, String reason, Instant dismissedAt) {}
}
