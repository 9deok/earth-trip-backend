package com.earthtrip.expense.adapter.out.persistence.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementSupplementJpaRepository
        extends JpaRepository<SettlementSupplementJpaEntity, String> {}
