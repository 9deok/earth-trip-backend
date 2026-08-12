package com.earthtrip.identity.adapter.out.persistence.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyConsentJpaRepository
        extends JpaRepository<PolicyConsentJpaEntity, PolicyConsentId> {

    List<PolicyConsentJpaEntity> findAllByUserId(String userId);
}
