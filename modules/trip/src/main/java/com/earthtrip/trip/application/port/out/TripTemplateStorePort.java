package com.earthtrip.trip.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TripTemplateStorePort {

    List<TemplateRecord> findAll(UUID ownerUserId);

    Optional<TemplateRecord> find(UUID templateId);

    TemplateRecord save(TemplateRecord template);

    Optional<DraftRecord> findDraft(UUID requestId);

    DraftRecord saveDraft(DraftRecord draft);

    record TemplateRecord(
        UUID id,
        UUID ownerUserId,
        UUID sourceTripId,
        String name,
        String description,
        Set<String> includeScopes,
        Map<String, Object> snapshot,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
    ) { }

    record DraftRecord(
        UUID requestId,
        UUID templateId,
        UUID tripId,
        UUID createdBy,
        Instant createdAt
    ) { }
}
