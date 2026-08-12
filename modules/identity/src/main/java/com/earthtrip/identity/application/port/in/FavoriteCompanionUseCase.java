package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FavoriteCompanionUseCase {

    List<FavoriteResult> list(UUID actorUserId);

    FavoriteResult add(
            UUID actorUserId,
            UUID requestId,
            UUID companionUserId,
            String displayName,
            String email);

    void remove(UUID actorUserId, UUID favoriteId);

    record FavoriteResult(
            UUID favoriteId,
            UUID companionUserId,
            String displayName,
            String email,
            Instant createdAt) {}
}
