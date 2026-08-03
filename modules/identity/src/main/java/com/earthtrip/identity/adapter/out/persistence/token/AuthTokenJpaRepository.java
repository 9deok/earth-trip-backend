package com.earthtrip.identity.adapter.out.persistence.token;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthTokenJpaRepository extends JpaRepository<AuthTokenJpaEntity, String> {

    Optional<AuthTokenJpaEntity> findByTokenHashAndPurposeAndConsumedAtIsNull(
        String tokenHash,
        String purpose
    );

    List<AuthTokenJpaEntity> findAllByUserIdAndPurposeAndConsumedAtIsNull(
        String userId,
        String purpose
    );
}
