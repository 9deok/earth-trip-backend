package com.earthtrip.planning.adapter.out.persistence.resource;
import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
interface PlanningUserStateJpaRepository extends JpaRepository<PlanningUserStateJpaEntity,PlanningUserStateId>{List<PlanningUserStateJpaEntity> findAllByResourceId(String resourceId);}
