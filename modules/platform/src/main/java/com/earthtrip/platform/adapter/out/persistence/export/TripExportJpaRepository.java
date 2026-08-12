package com.earthtrip.platform.adapter.out.persistence.export;

import org.springframework.data.jpa.repository.JpaRepository;

interface TripExportJpaRepository extends JpaRepository<TripExportJpaEntity, String> {}
