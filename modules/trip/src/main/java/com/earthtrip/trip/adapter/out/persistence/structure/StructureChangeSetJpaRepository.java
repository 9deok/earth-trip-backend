package com.earthtrip.trip.adapter.out.persistence.structure;

import org.springframework.data.jpa.repository.JpaRepository;

interface StructureChangeSetJpaRepository
    extends JpaRepository<StructureChangeSetJpaEntity, String> { }
