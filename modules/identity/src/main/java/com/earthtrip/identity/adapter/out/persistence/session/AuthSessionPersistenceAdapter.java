package com.earthtrip.identity.adapter.out.persistence.session;

import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.domain.AuthSession;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.earthtrip.identity.domain.UserId;
import org.springframework.stereotype.Component;

@Component
class AuthSessionPersistenceAdapter implements AuthSessionStorePort {

    private final AuthSessionJpaRepository repository;

    AuthSessionPersistenceAdapter(AuthSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionJpaEntity entity = repository.findById(session.id().toString())
            .map(existing -> {
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
