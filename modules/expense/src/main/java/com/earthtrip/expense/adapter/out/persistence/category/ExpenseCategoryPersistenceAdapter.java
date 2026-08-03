package com.earthtrip.expense.adapter.out.persistence.category;

import com.earthtrip.expense.application.port.out.ExpenseCategoryStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ExpenseCategoryPersistenceAdapter implements ExpenseCategoryStorePort {

    private final ExpenseCategoryJpaRepository repository;

    ExpenseCategoryPersistenceAdapter(ExpenseCategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CategoryRecord> findAll(UUID tripId) {
        return repository.findAllByTripIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(
                tripId.toString()
            ).stream()
            .map(ExpenseCategoryJpaEntity::toRecord)
            .toList();
    }

    @Override
    public Optional<CategoryRecord> findById(UUID categoryId) {
        return repository.findById(categoryId.toString())
            .map(ExpenseCategoryJpaEntity::toRecord)
            .filter(record -> record.deletedAt() == null);
    }

    @Override
    public Optional<CategoryRecord> findByCode(UUID tripId, String code) {
        return repository.findByTripIdAndCodeAndDeletedAtIsNull(tripId.toString(), code)
            .map(ExpenseCategoryJpaEntity::toRecord);
    }

    @Override
    public CategoryRecord save(CategoryRecord record) {
        ExpenseCategoryJpaEntity entity = repository.findById(record.id().toString())
            .map(current -> {
                current.apply(record);
                return current;
            })
            .orElseGet(() -> new ExpenseCategoryJpaEntity(record));
        return repository.saveAndFlush(entity).toRecord();
    }
}
