package com.earthtrip.wallet.adapter.out.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;

interface PackingTemplateApplicationJpaRepository
    extends JpaRepository<PackingTemplateApplicationJpaEntity, String> { }
