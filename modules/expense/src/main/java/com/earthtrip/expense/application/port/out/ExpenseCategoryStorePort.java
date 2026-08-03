package com.earthtrip.expense.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseCategoryStorePort {

    List<CategoryRecord> findAll(UUID tripId);

    Optional<CategoryRecord> findById(UUID categoryId);

    Optional<CategoryRecord> findByCode(UUID tripId, String code);

    CategoryRecord save(CategoryRecord record);

    record CategoryRecord(
        UUID id,
        UUID tripId,
        String code,
        String name,
        String color,
        int sortOrder,
        UUID createdBy,
        UUID updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
    ) { }
}
