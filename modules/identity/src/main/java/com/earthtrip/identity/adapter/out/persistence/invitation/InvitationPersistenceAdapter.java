package com.earthtrip.identity.adapter.out.persistence.invitation;

import com.earthtrip.identity.application.port.out.InvitationStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class InvitationPersistenceAdapter implements InvitationStorePort {
    private final InvitationJpaRepository repository;

    InvitationPersistenceAdapter(InvitationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InvitationRecord> findAll(UUID tripId) {
        return repository.findAllByTripIdOrderByCreatedAtDesc(tripId.toString()).stream()
                .map(InvitationJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<InvitationRecord> findById(UUID invitationId) {
        return repository.findById(invitationId.toString()).map(InvitationJpaEntity::toRecord);
    }

    @Override
    public Optional<InvitationRecord> findByTokenHash(String hash) {
        return repository.findByTokenHash(hash).map(InvitationJpaEntity::toRecord);
    }

    @Override
    public Optional<InvitationRecord> findActive(UUID tripId, String email) {
        return repository
                .findAllByTripIdAndEmailOrderByCreatedAtDesc(tripId.toString(), email)
                .stream()
                .map(InvitationJpaEntity::toRecord)
                .filter(record -> record.status().equals("PENDING"))
                .findFirst();
    }

    @Override
    public InvitationRecord save(InvitationRecord record) {
        InvitationJpaEntity entity =
                repository
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record);
                                    return existing;
                                })
                        .orElseGet(() -> new InvitationJpaEntity(record));
        return repository.saveAndFlush(entity).toRecord();
    }
}
