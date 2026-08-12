package com.earthtrip.expense.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryUseCase {

    List<CategoryResult> list(UUID tripId, UUID actorUserId);

    CategoryResult get(UUID tripId, UUID categoryId, UUID actorUserId);

    CategoryResult create(
            UUID tripId,
            UUID actorUserId,
            UUID requestId,
            String name,
            String color,
            Integer sortOrder);

    CategoryResult update(
            UUID tripId,
            UUID categoryId,
            UUID actorUserId,
            String name,
            String color,
            Integer sortOrder,
            long baseVersion);

    void delete(
            UUID tripId,
            UUID categoryId,
            UUID actorUserId,
            String replacementCode,
            long baseVersion);

    String requireCategoryCode(UUID tripId, String code);

    record CategoryResult(
            UUID categoryId,
            UUID tripId,
            String code,
            String name,
            String color,
            int sortOrder,
            boolean system,
            long version,
            UUID updatedBy,
            Instant updatedAt) {}
}
