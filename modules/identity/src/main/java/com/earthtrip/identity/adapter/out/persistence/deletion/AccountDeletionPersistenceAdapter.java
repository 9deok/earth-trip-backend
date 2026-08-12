package com.earthtrip.identity.adapter.out.persistence.deletion;

import com.earthtrip.identity.application.port.out.AccountDeletionStorePort;
import com.earthtrip.identity.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AccountDeletionPersistenceAdapter implements AccountDeletionStorePort {

    private final AccountDeletionJpaRepository repository;

    AccountDeletionPersistenceAdapter(AccountDeletionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeletionRecord createOrGet(UserId userId, Instant requestedAt, Instant scheduledAt) {
        AccountDeletionJpaEntity entity =
                pendingEntity(userId)
                        .findFirst()
                        .orElseGet(
                                () ->
                                        repository.save(
                                                new AccountDeletionJpaEntity(
                                                        UUID.randomUUID(),
                                                        userId.toString(),
                                                        requestedAt,
                                                        scheduledAt)));
        return new DeletionRecord(
                entity.id(), entity.requestedAt(), entity.scheduledDeletionAt(), entity.status());
    }

    @Override
    public Optional<DeletionRecord> findPending(UserId userId) {
        return pendingEntity(userId).findFirst().map(this::toRecord);
    }

    @Override
    public void cancel(UserId userId, Instant cancelledAt) {
        pendingEntity(userId)
                .findFirst()
                .ifPresent(
                        entity -> {
                            entity.cancel(cancelledAt);
                            repository.save(entity);
                        });
    }

    private java.util.stream.Stream<AccountDeletionJpaEntity> pendingEntity(UserId userId) {
        return repository.findAllByUserIdOrderByRequestedAtDesc(userId.toString()).stream()
                .filter(candidate -> candidate.status().equals("PENDING"));
    }

    private DeletionRecord toRecord(AccountDeletionJpaEntity entity) {
        return new DeletionRecord(
                entity.id(), entity.requestedAt(), entity.scheduledDeletionAt(), entity.status());
    }
}
