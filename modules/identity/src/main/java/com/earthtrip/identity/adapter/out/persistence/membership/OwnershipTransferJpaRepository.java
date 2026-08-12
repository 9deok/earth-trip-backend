package com.earthtrip.identity.adapter.out.persistence.membership;

import org.springframework.data.jpa.repository.JpaRepository;

interface OwnershipTransferJpaRepository
        extends JpaRepository<OwnershipTransferJpaEntity, String> {}
