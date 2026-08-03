package com.earthtrip.identity.adapter.out.persistence.membership;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TripMemberJpaRepository extends JpaRepository<TripMemberJpaEntity, String> {
    List<TripMemberJpaEntity> findAllByTripIdOrderByJoinedAtAsc(String tripId);
    Optional<TripMemberJpaEntity> findByTripIdAndUserId(String tripId, String userId);
    List<TripMemberJpaEntity> findAllByUserIdAndStatus(String userId, String status);
}
