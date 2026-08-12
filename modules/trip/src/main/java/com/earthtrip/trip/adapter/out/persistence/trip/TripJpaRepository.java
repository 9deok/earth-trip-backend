package com.earthtrip.trip.adapter.out.persistence.trip;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripJpaRepository extends JpaRepository<TripJpaEntity, String> {
    List<TripJpaEntity> findAllByOwnerUserIdOrderByUpdatedAtDesc(String ownerUserId);
}
