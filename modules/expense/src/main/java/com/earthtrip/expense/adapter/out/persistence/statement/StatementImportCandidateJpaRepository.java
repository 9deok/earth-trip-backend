package com.earthtrip.expense.adapter.out.persistence.statement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface StatementImportCandidateJpaRepository
        extends JpaRepository<StatementImportCandidateJpaEntity, String> {

    List<StatementImportCandidateJpaEntity> findAllByImportIdOrderByOccurredAtAsc(String importId);
}
