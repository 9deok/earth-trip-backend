package com.earthtrip.expense.adapter.out.persistence.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseAdjustmentJpaRepository
    extends JpaRepository<ExpenseAdjustmentJpaEntity, String> {

    List<ExpenseAdjustmentJpaEntity> findAllByExpenseIdOrderByCreatedAtAsc(String expenseId);
}
