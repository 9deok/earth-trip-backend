package com.earthtrip.identity.adapter.out.persistence.support;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SupportRequestJpaRepository extends JpaRepository<SupportRequestJpaEntity, String> {
    List<SupportRequestJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
