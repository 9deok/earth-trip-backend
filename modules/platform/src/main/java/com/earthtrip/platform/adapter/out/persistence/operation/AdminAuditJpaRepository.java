package com.earthtrip.platform.adapter.out.persistence.operation;

import org.springframework.data.jpa.repository.JpaRepository;

interface AdminAuditJpaRepository extends JpaRepository<AdminAuditJpaEntity, Long> {}
