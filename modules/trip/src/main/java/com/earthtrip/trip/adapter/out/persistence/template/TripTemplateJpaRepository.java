package com.earthtrip.trip.adapter.out.persistence.template;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripTemplateJpaRepository extends JpaRepository<TripTemplateJpaEntity, String> {
    List<TripTemplateJpaEntity> findAllByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            String ownerUserId);
}
