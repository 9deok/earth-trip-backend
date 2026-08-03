package com.earthtrip.identity.adapter.out.persistence.preference;

import org.springframework.data.jpa.repository.JpaRepository;

interface PreferenceJpaRepository extends JpaRepository<PreferenceJpaEntity, String> { }
