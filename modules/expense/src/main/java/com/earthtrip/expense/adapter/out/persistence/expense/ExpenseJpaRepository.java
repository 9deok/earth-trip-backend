package com.earthtrip.expense.adapter.out.persistence.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, String> {
    List<ExpenseJpaEntity> findAllByTripIdAndDeletedAtIsNullOrderByOccurredAtDescCreatedAtDesc(
            String trip);
}
