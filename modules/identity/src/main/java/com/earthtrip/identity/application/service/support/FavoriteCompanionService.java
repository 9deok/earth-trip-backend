package com.earthtrip.identity.application.service.support;

import com.earthtrip.identity.application.port.in.FavoriteCompanionUseCase;
import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class FavoriteCompanionService implements FavoriteCompanionUseCase {

    private final PersonalSupportStorePort store;
    private final UserAccountStorePort users;
    private final Clock clock;

    FavoriteCompanionService(
        PersonalSupportStorePort store,
        UserAccountStorePort users,
        Clock clock
    ) {
        this.store = store;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResult> list(UUID actorUserId) {
        return store.favorites(actorUserId).stream()
            .map(FavoriteCompanionService::result)
            .toList();
    }

    @Override
    public FavoriteResult add(
        UUID actorUserId,
        UUID requestId,
        UUID companionUserId,
        String displayName,
        String email
    ) {
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "요청 ID가 필요합니다.");
        }
        PersonalSupportStorePort.FavoriteRecord byId = store.favorite(requestId).orElse(null);
        if (byId != null) {
            if (!byId.userId().equals(actorUserId)) {
                throw idempotencyConflict();
            }
            return result(byId);
        }
        ResolvedCompanion companion = resolveCompanion(
            actorUserId, companionUserId, displayName, email
        );
        PersonalSupportStorePort.FavoriteRecord duplicate = companion.userId() == null
            ? store.favoriteByEmail(actorUserId, companion.email()).orElse(null)
            : store.favoriteByCompanion(actorUserId, companion.userId()).orElse(null);
        if (duplicate != null) {
            return result(duplicate);
        }
        return result(store.saveFavorite(new PersonalSupportStorePort.FavoriteRecord(
            requestId, actorUserId, companion.userId(), companion.displayName(),
            companion.email(), clock.instant()
        )));
    }

    @Override
    public void remove(UUID actorUserId, UUID favoriteId) {
        PersonalSupportStorePort.FavoriteRecord favorite = store.favorite(favoriteId)
            .filter(candidate -> candidate.userId().equals(actorUserId))
            .orElseThrow(() -> EarthTripException.notFound(
                "FAVORITE_COMPANION_NOT_FOUND",
                "즐겨찾는 동행자를 찾을 수 없습니다."
            ));
        store.deleteFavorite(favorite.id());
    }

    private ResolvedCompanion resolveCompanion(
        UUID actorUserId,
        UUID companionUserId,
        String displayName,
        String email
    ) {
        if (companionUserId != null) {
            if (companionUserId.equals(actorUserId)) {
                throw EarthTripException.badRequest(
                    "CANNOT_FAVORITE_SELF",
                    "자기 자신은 동행자 즐겨찾기에 추가할 수 없습니다."
                );
            }
            UserAccount account = users.findById(new UserId(companionUserId))
                .filter(candidate -> candidate.status() != UserAccount.Status.DELETED)
                .orElseThrow(() -> EarthTripException.notFound(
                    "COMPANION_ACCOUNT_NOT_FOUND",
                    "동행자 계정을 찾을 수 없습니다."
                ));
            return new ResolvedCompanion(
                account.id().value(), account.displayName(), account.email().value()
            );
        }
        EmailAddress emailAddress;
        try {
            emailAddress = new EmailAddress(email);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw EarthTripException.badRequest(
                "INVALID_COMPANION_EMAIL",
                "동행자 이메일을 확인해 주세요."
            );
        }
        String safeDisplayName = displayName(displayName);
        UserAccount registered = users.findByEmail(emailAddress).orElse(null);
        if (registered != null && registered.id().value().equals(actorUserId)) {
            throw EarthTripException.badRequest(
                "CANNOT_FAVORITE_SELF",
                "자기 자신은 동행자 즐겨찾기에 추가할 수 없습니다."
            );
        }
        return new ResolvedCompanion(
            registered == null ? null : registered.id().value(),
            registered == null ? safeDisplayName : registered.displayName(),
            emailAddress.value()
        );
    }

    private static String displayName(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 80) {
            throw EarthTripException.badRequest(
                "INVALID_COMPANION_NAME",
                "동행자 표시 이름은 1자 이상 80자 이하여야 합니다."
            );
        }
        return value.strip();
    }

    private static FavoriteResult result(PersonalSupportStorePort.FavoriteRecord record) {
        return new FavoriteResult(
            record.id(), record.companionId(), record.displayName(),
            record.email(), record.createdAt()
        );
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict(
            "IDEMPOTENCY_KEY_REUSED",
            "이미 다른 즐겨찾기에 사용된 요청 ID입니다."
        );
    }

    private record ResolvedCompanion(UUID userId, String displayName, String email) { }
}
