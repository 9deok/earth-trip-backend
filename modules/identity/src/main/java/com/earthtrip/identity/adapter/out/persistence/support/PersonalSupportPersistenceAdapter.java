package com.earthtrip.identity.adapter.out.persistence.support;

import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PersonalSupportPersistenceAdapter implements PersonalSupportStorePort {

    private final FavoriteCompanionJpaRepository favorites;
    private final SupportRequestJpaRepository supportRequests;

    PersonalSupportPersistenceAdapter(
            FavoriteCompanionJpaRepository favorites, SupportRequestJpaRepository supportRequests) {
        this.favorites = favorites;
        this.supportRequests = supportRequests;
    }

    @Override
    public List<FavoriteRecord> favorites(UUID userId) {
        return favorites.findAllByUserIdOrderByCreatedAtDesc(userId.toString()).stream()
                .map(FavoriteCompanionJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<FavoriteRecord> favorite(UUID favoriteId) {
        return favorites.findById(favoriteId.toString()).map(FavoriteCompanionJpaEntity::toRecord);
    }

    @Override
    public Optional<FavoriteRecord> favoriteByCompanion(UUID userId, UUID companionId) {
        return favorites
                .findByUserIdAndCompanionId(userId.toString(), companionId.toString())
                .map(FavoriteCompanionJpaEntity::toRecord);
    }

    @Override
    public Optional<FavoriteRecord> favoriteByEmail(UUID userId, String email) {
        return favorites
                .findByUserIdAndEmail(userId.toString(), email)
                .map(FavoriteCompanionJpaEntity::toRecord);
    }

    @Override
    public FavoriteRecord saveFavorite(FavoriteRecord record) {
        return favorites.save(new FavoriteCompanionJpaEntity(record)).toRecord();
    }

    @Override
    public void deleteFavorite(UUID favoriteId) {
        favorites.deleteById(favoriteId.toString());
    }

    @Override
    public Optional<SupportRecord> support(UUID supportRequestId) {
        return supportRequests
                .findById(supportRequestId.toString())
                .map(SupportRequestJpaEntity::toRecord);
    }

    @Override
    public List<SupportRecord> supports(UUID userId) {
        return supportRequests.findAllByUserIdOrderByCreatedAtDesc(userId.toString()).stream()
                .map(SupportRequestJpaEntity::toRecord)
                .toList();
    }

    @Override
    public SupportRecord saveSupport(SupportRecord record) {
        return supportRequests.save(new SupportRequestJpaEntity(record)).toRecord();
    }
}
