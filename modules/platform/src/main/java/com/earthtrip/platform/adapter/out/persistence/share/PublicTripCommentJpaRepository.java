package com.earthtrip.platform.adapter.out.persistence.share;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PublicTripCommentJpaRepository extends JpaRepository<PublicTripCommentJpaEntity, String> {
    List<PublicTripCommentJpaEntity> findByPublicationIdAndStatusOrderByCreatedAtDesc(
            String publicationId, String status, Pageable pageable);

    long countByPublicationIdAndStatus(String publicationId, String status);
}
