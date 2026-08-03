package com.earthtrip.identity.adapter.out.persistence.account;

import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class UserAccountPersistenceAdapter implements UserAccountStorePort {

    private final UserJpaRepository repository;

    UserAccountPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findById(UserId userId) {
        return repository.findById(userId.toString()).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(EmailAddress email) {
        return repository.findByEmail(email.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserJpaEntity entity = repository.findById(account.id().toString())
            .map(existing -> {
                existing.apply(account);
                return existing;
            })
            .orElseGet(() -> UserJpaEntity.from(account));
        return repository.saveAndFlush(entity).toDomain();
    }
}
