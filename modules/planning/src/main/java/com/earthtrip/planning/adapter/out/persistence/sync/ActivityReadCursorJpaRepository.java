package com.earthtrip.planning.adapter.out.persistence.sync;

import org.springframework.data.jpa.repository.JpaRepository;

interface ActivityReadCursorJpaRepository
    extends JpaRepository<ActivityReadCursorJpaEntity, ActivityReadCursorId> { }
