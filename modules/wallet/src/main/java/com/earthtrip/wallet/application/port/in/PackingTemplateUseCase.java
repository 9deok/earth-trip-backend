package com.earthtrip.wallet.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PackingTemplateUseCase {

    List<TemplateResult> list(UUID actorUserId);

    TemplateResult get(UUID templateId, UUID actorUserId);

    TemplateResult create(UUID actorUserId, TemplateCommand command);

    TemplateResult update(UUID templateId, UUID actorUserId, TemplateCommand command);

    void delete(UUID templateId, UUID actorUserId, long baseVersion);

    ApplicationResult apply(UUID tripId, UUID actorUserId, ApplicationCommand command);

    record TemplateCommand(
            UUID requestId,
            String name,
            String visibility,
            List<TemplateItem> items,
            long baseVersion) {}

    record TemplateItem(String name, String category, int quantity, String note) {}

    record TemplateResult(
            UUID templateId,
            UUID ownerUserId,
            String name,
            String visibility,
            List<TemplateItem> items,
            boolean editable,
            long version,
            Instant createdAt,
            Instant updatedAt) {}

    record ApplicationCommand(UUID requestId, UUID templateId, String visibility) {}

    record ApplicationResult(
            UUID applicationId, UUID templateId, List<UUID> packingItemIds, Instant appliedAt) {}
}
