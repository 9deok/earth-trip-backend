package com.earthtrip.identity.adapter.out.persistence.invitation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, String> {
    List<InvitationJpaEntity> findAllByTripIdOrderByCreatedAtDesc(String tripId);

    Optional<InvitationJpaEntity> findByTokenHash(String tokenHash);

    List<InvitationJpaEntity> findAllByTripIdAndEmailOrderByCreatedAtDesc(
            String tripId, String email);
}
