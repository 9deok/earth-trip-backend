package com.earthtrip.identity.adapter.out.persistence.support;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface FavoriteCompanionJpaRepository extends JpaRepository<FavoriteCompanionJpaEntity, String> {

    List<FavoriteCompanionJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<FavoriteCompanionJpaEntity> findByUserIdAndCompanionId(
            String userId, String companionId);

    Optional<FavoriteCompanionJpaEntity> findByUserIdAndEmail(String userId, String email);
}
