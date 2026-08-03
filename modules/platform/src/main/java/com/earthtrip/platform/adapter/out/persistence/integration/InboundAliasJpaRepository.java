package com.earthtrip.platform.adapter.out.persistence.integration;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface InboundAliasJpaRepository extends JpaRepository<InboundAliasJpaEntity, String> {

    List<InboundAliasJpaEntity> findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(
        String userId
    );

    Optional<InboundAliasJpaEntity> findByAliasAndRevokedAtIsNull(String alias);
}
