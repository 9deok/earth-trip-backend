package com.earthtrip.identity.adapter.out.persistence.session;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, String> {

    Optional<AuthSessionJpaEntity> findByAccessTokenHash(String accessTokenHash);

    Optional<AuthSessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthSessionJpaEntity> findAllByUserIdOrderByLastUsedAtDesc(String userId);
}
