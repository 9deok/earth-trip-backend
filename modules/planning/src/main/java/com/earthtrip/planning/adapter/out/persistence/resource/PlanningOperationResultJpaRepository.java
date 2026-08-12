package com.earthtrip.planning.adapter.out.persistence.resource;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlanningOperationResultJpaRepository
        extends JpaRepository<PlanningOperationResultJpaEntity, String> {}
