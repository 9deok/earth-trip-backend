package com.earthtrip.planning.adapter.out.persistence.change;

import org.springframework.data.jpa.repository.JpaRepository;

interface ScheduleChangeSetJpaRepository
    extends JpaRepository<ScheduleChangeSetJpaEntity, String> { }
