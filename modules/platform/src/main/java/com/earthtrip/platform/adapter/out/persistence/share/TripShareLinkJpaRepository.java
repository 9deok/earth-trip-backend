package com.earthtrip.platform.adapter.out.persistence.share;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripShareLinkJpaRepository extends JpaRepository<TripShareLinkJpaEntity, String> {
    List<TripShareLinkJpaEntity> findAllByTripIdOrderByCreatedAtDesc(String tripId);

    Optional<TripShareLinkJpaEntity> findByTokenHash(String tokenHash);
}
