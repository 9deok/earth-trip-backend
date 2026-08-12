package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalSupportStorePort {

    List<FavoriteRecord> favorites(UUID userId);

    Optional<FavoriteRecord> favorite(UUID favoriteId);

    Optional<FavoriteRecord> favoriteByCompanion(UUID userId, UUID companionId);

    Optional<FavoriteRecord> favoriteByEmail(UUID userId, String email);

    FavoriteRecord saveFavorite(FavoriteRecord record);

    void deleteFavorite(UUID favoriteId);

    Optional<SupportRecord> support(UUID supportRequestId);

    List<SupportRecord> supports(UUID userId);

    SupportRecord saveSupport(SupportRecord record);

    record FavoriteRecord(
            UUID id,
            UUID userId,
            UUID companionId,
            String displayName,
            String email,
            Instant createdAt) {}

    record SupportRecord(
            UUID id,
            UUID userId,
            String category,
            String description,
            String traceId,
            String diagnosticsJson,
            String status,
            Instant createdAt) {}
}
