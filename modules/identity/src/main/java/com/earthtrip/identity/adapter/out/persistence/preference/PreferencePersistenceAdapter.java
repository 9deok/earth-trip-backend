package com.earthtrip.identity.adapter.out.persistence.preference;

import com.earthtrip.identity.application.port.out.PreferenceStorePort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PreferencePersistenceAdapter implements PreferenceStorePort {

    private final PreferenceJpaRepository repository;

    PreferencePersistenceAdapter(PreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PreferenceRecord> find(UUID userId) {
        return repository.findById(userId.toString()).map(PreferenceJpaEntity::toRecord);
    }

    @Override
    public PreferenceRecord save(PreferenceRecord preference) {
        PreferenceJpaEntity entity =
                repository
                        .findById(preference.userId().toString())
                        .map(
                                existing -> {
                                    existing.apply(preference);
                                    return existing;
                                })
                        .orElseGet(() -> new PreferenceJpaEntity(preference));
        return repository.saveAndFlush(entity).toRecord();
    }
}
