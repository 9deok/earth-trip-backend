package com.earthtrip.platform.adapter.out.persistence.share;

import org.springframework.data.jpa.repository.JpaRepository;

interface TripSharePasswordSessionJpaRepository
        extends JpaRepository<TripSharePasswordSessionJpaEntity, String> {}
