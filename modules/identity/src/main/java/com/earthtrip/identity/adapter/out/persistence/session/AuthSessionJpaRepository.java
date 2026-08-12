package com.earthtrip.identity.adapter.out.persistence.session;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, String> {

    Optional<AuthSessionJpaEntity> findByAccessTokenHash(String accessTokenHash);

    @Query(
            """
        select new com.earthtrip.identity.adapter.out.persistence.session.AuthSessionAuthenticationRow(
               session.id,
               session.userId,
               session.accessTokenHash,
               session.refreshTokenHash,
               session.deviceName,
               session.accessExpiresAt,
               session.refreshExpiresAt,
               session.lastUsedAt,
               session.revokedAt,
               session.createdAt,
               account.displayName,
               account.status)
          from AuthSessionJpaEntity session, UserJpaEntity account
         where session.userId = account.id
           and session.accessTokenHash = :accessTokenHash
        """)
    Optional<AuthSessionAuthenticationRow> findAuthenticationByAccessTokenHash(
            @Param("accessTokenHash") String accessTokenHash);

    Optional<AuthSessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthSessionJpaEntity> findAllByUserIdOrderByLastUsedAtDesc(String userId);
}
