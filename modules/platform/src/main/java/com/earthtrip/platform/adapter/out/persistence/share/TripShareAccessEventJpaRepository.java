package com.earthtrip.platform.adapter.out.persistence.share;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripShareAccessEventJpaRepository
        extends JpaRepository<TripShareAccessEventJpaEntity, Long> {
    List<TripShareAccessEventJpaEntity> findAllByShareIdOrderByOccurredAtDesc(String shareId);
}
