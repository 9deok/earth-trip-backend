package com.earthtrip.identity.adapter.out.persistence.session;

import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AuthSessionPersistenceAdapter implements AuthSessionStorePort {

    private final AuthSessionJpaRepository repository;
    private final AuthSessionQuerydslSupport querydsl;

    AuthSessionPersistenceAdapter(
            AuthSessionJpaRepository repository, AuthSessionQuerydslSupport querydsl) {
        this.repository = repository;
        this.querydsl = querydsl;
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionJpaEntity entity =
                repository
                        .findById(session.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(session);
                                    return existing;
                                })
                        .orElseGet(() -> AuthSessionJpaEntity.from(session));
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<AuthSession> findById(UUID sessionId) {
        return repository.findById(sessionId.toString()).map(AuthSessionJpaEntity::toDomain);
    }

    @Override
    public Optional<AuthSession> findByAccessTokenHash(String tokenHash) {
        return repository.findByAccessTokenHash(tokenHash).map(AuthSessionJpaEntity::toDomain);
    }

    @Override
    public Optional<AuthenticatedSessionRecord> findAuthenticationByAccessTokenHash(
            String tokenHash) {
        return querydsl.findAuthenticationByAccessTokenHash(tokenHash)
                .map(
                        row ->
                                new AuthenticatedSessionRecord(
                                        row.toDomain(), row.displayName(), row.accountCanSignIn()));
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String tokenHash) {
        return repository.findByRefreshTokenHash(tokenHash).map(AuthSessionJpaEntity::toDomain);
    }

    @Override
    public List<AuthSession> findByUserId(UserId userId) {
        return repository.findAllByUserIdOrderByLastUsedAtDesc(userId.toString()).stream()
                .map(AuthSessionJpaEntity::toDomain)
                .toList();
    }
}
