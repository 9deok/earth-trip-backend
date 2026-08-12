package com.earthtrip.expense.adapter.out.persistence.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseCategoryJpaRepository extends JpaRepository<ExpenseCategoryJpaEntity, String> {

    List<ExpenseCategoryJpaEntity> findAllByTripIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(
            String tripId);

    Optional<ExpenseCategoryJpaEntity> findByTripIdAndCodeAndDeletedAtIsNull(
            String tripId, String code);
}
