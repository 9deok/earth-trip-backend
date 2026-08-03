package com.earthtrip.expense.adapter.out.persistence.statement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface StatementImportJpaRepository extends JpaRepository<StatementImportJpaEntity, String> {

    List<StatementImportJpaEntity> findAllByTripIdOrderByCreatedAtDesc(String tripId);
}
