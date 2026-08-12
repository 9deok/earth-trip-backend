package com.earthtrip.trip.adapter.out.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;

interface TripTemplateDraftJpaRepository
        extends JpaRepository<TripTemplateDraftJpaEntity, String> {}
