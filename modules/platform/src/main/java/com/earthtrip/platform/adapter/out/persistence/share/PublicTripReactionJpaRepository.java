package com.earthtrip.platform.adapter.out.persistence.share;

import org.springframework.data.jpa.repository.JpaRepository;

interface PublicTripReactionJpaRepository
        extends JpaRepository<PublicTripReactionJpaEntity, PublicTripReactionJpaId> {
    long countByIdPublicationIdAndIdReactionType(String publicationId, String reactionType);
}
