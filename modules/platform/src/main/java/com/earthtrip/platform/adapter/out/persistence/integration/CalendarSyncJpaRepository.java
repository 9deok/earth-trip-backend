package com.earthtrip.platform.adapter.out.persistence.integration;

import org.springframework.data.jpa.repository.JpaRepository;

interface CalendarSyncJpaRepository extends JpaRepository<CalendarSyncJpaEntity, String> {}
