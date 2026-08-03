package com.earthtrip.expense.adapter.out.persistence.review;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseReviewDayJpaRepository
    extends JpaRepository<ExpenseReviewDayJpaEntity, ExpenseReviewDayId> {

    List<ExpenseReviewDayJpaEntity> findAllByTripIdOrderByLocalDateAsc(String tripId);

    default ExpenseReviewDayId id(String tripId, LocalDate localDate) {
        return new ExpenseReviewDayId(tripId, localDate);
    }
}
