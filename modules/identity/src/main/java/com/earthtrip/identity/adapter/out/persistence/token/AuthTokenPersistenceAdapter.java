package com.earthtrip.identity.adapter.out.persistence.token;

import com.earthtrip.identity.application.port.out.AuthTokenStorePort;
import com.earthtrip.identity.domain.AuthToken;
import com.earthtrip.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class AuthTokenPersistenceAdapter implements AuthTokenStorePort {

    private final AuthTokenJpaRepository repository;

    AuthTokenPersistenceAdapter(AuthTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthToken save(AuthToken token) {
        AuthTokenJpaEntity entity = repository.findById(token.id().toString())
            .map(existing -> {
                existing.apply(token);
                return existing;
            })
            .orElseGet(() -> AuthTokenJpaEntity.from(token));
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<AuthToken> findUsableByHash(String tokenHash, AuthToken.Purpose purpose) {
        return repository.findByTokenHashAndPurposeAndConsumedAtIsNull(tokenHash, purpose.name())
            .map(AuthTokenJpaEntity::toDomain);
    }

    @Override
    public void invalidateFor(UserId userId, AuthToken.Purpose purpose, Instant now) {
        repository.findAllByUserIdAndPurposeAndConsumedAtIsNull(userId.toString(), purpose.name())
            .forEach(entity -> {
                AuthToken token = entity.toDomain();
                try {
                    token.consume(now);
                } catch (IllegalStateException ignored) {
                    // Expired tokens are already unusable and may remain for audit retention.
                }
                entity.apply(token);
            });
    }
}
