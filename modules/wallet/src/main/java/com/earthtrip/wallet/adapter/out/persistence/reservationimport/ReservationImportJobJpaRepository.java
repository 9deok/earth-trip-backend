package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationImportJobJpaRepository
        extends JpaRepository<ReservationImportJobJpaEntity, String> {}
