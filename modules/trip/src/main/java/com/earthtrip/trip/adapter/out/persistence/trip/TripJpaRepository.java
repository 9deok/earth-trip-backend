package com.earthtrip.trip.adapter.out.persistence.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface TripJpaRepository extends JpaRepository<TripJpaEntity, String> {
    List<TripJpaEntity> findAllByOwnerUserIdOrderByUpdatedAtDesc(String ownerUserId);
}
