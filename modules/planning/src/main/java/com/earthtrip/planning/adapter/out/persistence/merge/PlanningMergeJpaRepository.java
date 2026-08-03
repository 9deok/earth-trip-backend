package com.earthtrip.planning.adapter.out.persistence.merge;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlanningMergeJpaRepository extends JpaRepository<PlanningMergeJpaEntity, String> { }
