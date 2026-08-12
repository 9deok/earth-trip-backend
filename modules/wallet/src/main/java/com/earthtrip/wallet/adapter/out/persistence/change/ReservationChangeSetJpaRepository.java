package com.earthtrip.wallet.adapter.out.persistence.change;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationChangeSetJpaRepository
        extends JpaRepository<ReservationChangeSetJpaEntity, String> {}
