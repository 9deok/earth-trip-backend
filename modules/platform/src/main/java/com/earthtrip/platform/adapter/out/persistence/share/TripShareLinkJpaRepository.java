package com.earthtrip.platform.adapter.out.persistence.share;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripShareLinkJpaRepository extends JpaRepository<TripShareLinkJpaEntity, String> {
    List<TripShareLinkJpaEntity> findAllByTripIdOrderByCreatedAtDesc(String tripId);

    List<TripShareLinkJpaEntity> findTop50ByStatusAndVisibilityOrderByUpdatedAtDesc(
            String status, String visibility);

    Optional<TripShareLinkJpaEntity> findByTokenHash(String tokenHash);
}
