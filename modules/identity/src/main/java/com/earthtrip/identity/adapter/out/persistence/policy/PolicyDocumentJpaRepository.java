package com.earthtrip.identity.adapter.out.persistence.policy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PolicyDocumentJpaRepository extends JpaRepository<PolicyDocumentJpaEntity, String> {

    List<PolicyDocumentJpaEntity> findAllByActiveTrueOrderByPublishedAtAsc();

    Optional<PolicyDocumentJpaEntity> findByIdAndActiveTrue(String id);
}
