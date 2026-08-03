package com.earthtrip.trip.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TripTemplateUseCase {

    List<TemplateResult> list(UUID actorUserId);

    TemplateResult create(UUID actorUserId, CreateCommand command);

    TemplateResult get(UUID templateId, UUID actorUserId);

    TemplateResult update(UUID templateId, UUID actorUserId, UpdateCommand command);

    void delete(UUID templateId, UUID actorUserId, long baseVersion);

    TripManagementUseCase.TripResult createDraft(
        UUID templateId,
        UUID actorUserId,
        DraftCommand command
    );

    record CreateCommand(
        UUID requestId,
        UUID sourceTripId,
        String name,
        String description,
        Set<String> includeScopes
    ) { }

    record UpdateCommand(
        String name,
        String description,
        Set<String> includeScopes,
        long baseVersion
    ) { }

    record DraftCommand(
        UUID requestId,
        String title,
        LocalDate startDate,
        String timeZone,
        String defaultCurrency
    ) { }

    record TemplateResult(
        UUID templateId,
        UUID sourceTripId,
        String name,
        String description,
        Set<String> includeScopes,
        int segmentCount,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) { }
}
