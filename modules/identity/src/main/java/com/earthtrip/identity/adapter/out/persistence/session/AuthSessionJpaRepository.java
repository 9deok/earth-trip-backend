package com.earthtrip.identity.adapter.out.persistence.session;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, String> {

    Optional<AuthSessionJpaEntity> findByAccessTokenHash(String accessTokenHash);

    Optional<AuthSessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthSessionJpaEntity> findAllByUserIdOrderByLastUsedAtDesc(String userId);
}
