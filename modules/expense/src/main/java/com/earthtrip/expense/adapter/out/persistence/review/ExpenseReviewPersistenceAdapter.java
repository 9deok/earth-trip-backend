package com.earthtrip.expense.adapter.out.persistence.review;

import com.earthtrip.expense.application.port.out.ExpenseReviewStorePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ExpenseReviewPersistenceAdapter implements ExpenseReviewStorePort {

    private final ExpenseReviewDayJpaRepository repository;

    ExpenseReviewPersistenceAdapter(ExpenseReviewDayJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ReviewRecord> findAll(UUID tripId) {
        return repository.findAllByTripIdOrderByLocalDateAsc(tripId.toString()).stream()
                .map(ExpenseReviewDayJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<ReviewRecord> find(UUID tripId, LocalDate localDate) {
        return repository
                .findById(new ExpenseReviewDayId(tripId.toString(), localDate))
                .map(ExpenseReviewDayJpaEntity::toRecord);
    }

    @Override
    public ReviewRecord save(ReviewRecord record) {
        ExpenseReviewDayId id =
                new ExpenseReviewDayId(record.tripId().toString(), record.localDate());
        ExpenseReviewDayJpaEntity entity =
                repository
                        .findById(id)
                        .map(
                                existing -> {
                                    existing.apply(record);
                                    return existing;
                                })
                        .orElseGet(() -> new ExpenseReviewDayJpaEntity(record));
        return repository.saveAndFlush(entity).toRecord();
    }

    @Override
    public void delete(UUID tripId, LocalDate localDate) {
        repository.deleteById(new ExpenseReviewDayId(tripId.toString(), localDate));
    }
}
