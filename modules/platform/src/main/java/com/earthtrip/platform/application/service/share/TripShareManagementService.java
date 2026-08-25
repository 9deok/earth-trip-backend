package com.earthtrip.platform.application.service.share;

import static com.earthtrip.platform.application.service.share.TripSharePolicy.SCOPES;

import com.earthtrip.platform.application.port.in.TripShareManagementUseCase;
import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import com.earthtrip.platform.application.port.out.ShareCredentialPort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripShareManagementService implements TripShareManagementUseCase {

    private final TripAccess access;
    private final TripShareStorePort store;
    private final ShareCredentialPort credentials;
    private final Clock clock;
    private final PublicTripEngagementStorePort engagement;

    TripShareManagementService(
            TripAccess access,
            TripShareStorePort store,
            ShareCredentialPort credentials,
            Clock clock,
            PublicTripEngagementStorePort engagement) {
        this.access = access;
        this.store = store;
        this.credentials = credentials;
        this.clock = clock;
        this.engagement = engagement;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkResult> list(UUID tripId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        return store.findAll(tripId).stream().map(record -> result(record, null)).toList();
    }

    @Override
    public ShareLinkResult create(UUID tripId, UUID actorUserId, ShareLinkCommand command) {
        TripAccess.AccessResult tripAccess = access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        TripShareStorePort.ShareRecord existing = store.findById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 공유 링크에 사용된 요청 ID입니다.");
            }
            return result(existing, null);
        }
        String token = credentials.newToken();
        String passwordHash = password(command.password());
        String visibility = visibility(command.visibility(), passwordHash);
        Instant now = clock.instant();
        TripShareStorePort.ShareRecord saved =
                store.save(
                        new TripShareStorePort.ShareRecord(
                                command.requestId(),
                                tripId,
                                credentials.hashToken(token),
                                name(command.name()),
                                scopes(command.scopes()),
                                passwordHash,
                                tripAccess.ownerUserId(),
                                visibility,
                                publicNote(command.publicNote()),
                                publicContent(command.publicContent()),
                                expiry(command.expiresAt(), now),
                                "ACTIVE",
                                actorUserId,
                                now,
                                now,
                                null,
                                0));
        return result(saved, token);
    }

    @Override
    public ShareLinkResult update(
            UUID tripId, UUID shareId, UUID actorUserId, ShareLinkCommand command) {
        access.requireEditor(tripId, actorUserId);
        TripShareStorePort.ShareRecord current = load(tripId, shareId);
        version(current, command.baseVersion());
        if (!current.status().equals("ACTIVE")) {
            throw EarthTripException.conflict("SHARE_LINK_REVOKED", "회수된 공유 링크는 수정할 수 없습니다.");
        }
        String passwordHash =
                Boolean.TRUE.equals(command.removePassword())
                        ? null
                        : command.password() == null
                                ? current.passwordHash()
                                : password(command.password());
        String visibility =
                command.visibility() == null
                        ? current.visibility()
                        : visibility(command.visibility(), passwordHash);
        if (visibility.equals("PUBLIC") && passwordHash != null) {
            throw EarthTripException.badRequest(
                    "PUBLIC_SHARE_PASSWORD_NOT_ALLOWED", "전체 공개 계획에는 비밀번호를 설정할 수 없습니다.");
        }
        Instant now = clock.instant();
        TripShareStorePort.ShareRecord saved =
                store.save(
                        new TripShareStorePort.ShareRecord(
                                current.id(),
                                tripId,
                                current.tokenHash(),
                                command.name() == null ? current.name() : name(command.name()),
                                command.scopes() == null
                                        ? current.scopes()
                                        : scopes(command.scopes()),
                                passwordHash,
                                current.projectionUserId(),
                                visibility,
                                command.publicNote() == null
                                        ? current.publicNote()
                                        : publicNote(command.publicNote()),
                                command.publicContent() == null
                                        ? current.publicContent()
                                        : publicContent(command.publicContent()),
                                Boolean.TRUE.equals(command.removeExpiry())
                                        ? null
                                        : command.expiresAt() == null
                                                ? current.expiresAt()
                                                : expiry(command.expiresAt(), now),
                                current.status(),
                                current.createdBy(),
                                current.createdAt(),
                                now,
                                current.revokedAt(),
                                current.version()));
        return result(saved, null);
    }

    @Override
    public void revoke(UUID tripId, UUID shareId, UUID actorUserId, long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        TripShareStorePort.ShareRecord current = load(tripId, shareId);
        if (current.status().equals("REVOKED")) {
            return;
        }
        version(current, baseVersion);
        Instant now = clock.instant();
        store.save(
                new TripShareStorePort.ShareRecord(
                        current.id(),
                        tripId,
                        current.tokenHash(),
                        current.name(),
                        current.scopes(),
                        current.passwordHash(),
                        current.projectionUserId(),
                        current.visibility(),
                        current.publicNote(),
                        current.publicContent(),
                        current.expiresAt(),
                        "REVOKED",
                        current.createdBy(),
                        current.createdAt(),
                        now,
                        now,
                        current.version()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessEventResult> accessEvents(UUID tripId, UUID shareId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        load(tripId, shareId);
        return store.accessEvents(shareId).stream()
                .limit(1_000)
                .map(
                        record ->
                                new AccessEventResult(
                                        record.eventId(),
                                        record.success(),
                                        record.reason(),
                                        record.occurredAt()))
                .toList();
    }

    private TripShareStorePort.ShareRecord load(UUID tripId, UUID shareId) {
        return store.findById(shareId)
                .filter(share -> share.tripId().equals(tripId))
                .orElseThrow(TripShareManagementService::notFound);
    }

    private String password(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() < 4 || value.length() > 128) {
            throw EarthTripException.badRequest("INVALID_SHARE_PASSWORD", "공유 비밀번호는 4~128자여야 합니다.");
        }
        return credentials.encodePassword(value);
    }

    private static String name(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw EarthTripException.badRequest("INVALID_SHARE_NAME", "공유 링크 이름은 1~120자여야 합니다.");
        }
        return value.strip();
    }

    private static List<String> scopes(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw EarthTripException.badRequest("SHARE_SCOPE_REQUIRED", "공유할 범위를 하나 이상 선택해 주세요.");
        }
        List<String> normalized =
                values.stream()
                        .map(value -> value.strip().toUpperCase(Locale.ROOT))
                        .distinct()
                        .sorted()
                        .toList();
        if (!SCOPES.containsAll(normalized)) {
            throw EarthTripException.badRequest("INVALID_SHARE_SCOPE", "지원하지 않는 공유 범위가 포함되어 있습니다.");
        }
        return normalized;
    }

    private static String visibility(String value, String passwordHash) {
        String normalized =
                value == null || value.isBlank()
                        ? "LINK_ONLY"
                        : value.strip().toUpperCase(Locale.ROOT);
        if (!TripSharePolicy.VISIBILITIES.contains(normalized)) {
            throw EarthTripException.badRequest("INVALID_SHARE_VISIBILITY", "지원하지 않는 공개 방식입니다.");
        }
        if (normalized.equals("PUBLIC") && passwordHash != null) {
            throw EarthTripException.badRequest(
                    "PUBLIC_SHARE_PASSWORD_NOT_ALLOWED", "전체 공개 계획에는 비밀번호를 설정할 수 없습니다.");
        }
        return normalized;
    }

    private static String publicNote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 500) {
            throw EarthTripException.badRequest("PUBLIC_NOTE_TOO_LONG", "공개 메모는 500자 이하여야 합니다.");
        }
        return normalized;
    }

    private static Map<String, String> publicContent(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (!TripSharePolicy.PUBLIC_CONTENT_FIELDS.containsAll(values.keySet())) {
            throw EarthTripException.badRequest(
                    "INVALID_PUBLIC_CONTENT_FIELD", "지원하지 않는 공개 콘텐츠 항목이 포함되어 있습니다.");
        }
        Map<String, String> normalized = new java.util.LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    if (value == null || value.isBlank()) {
                        return;
                    }
                    String text = value.strip();
                    if (text.length() > 1_000) {
                        throw EarthTripException.badRequest(
                                "PUBLIC_CONTENT_TOO_LONG", "공개 콘텐츠 항목은 각각 1,000자 이하여야 합니다.");
                    }
                    if ((key.equals("heroPhotoUrl") || key.equals("galleryPhotoUrl"))
                            && !text.startsWith("https://")) {
                        throw EarthTripException.badRequest(
                                "INVALID_PUBLIC_PHOTO_URL", "공개 사진은 https 주소여야 합니다.");
                    }
                    normalized.put(key, text);
                });
        return Map.copyOf(normalized);
    }

    private static Instant expiry(Instant value, Instant now) {
        if (value != null && !value.isAfter(now)) {
            throw EarthTripException.badRequest(
                    "INVALID_SHARE_EXPIRY", "공유 링크 만료 시각은 현재보다 뒤여야 합니다.");
        }
        return value;
    }

    private ShareLinkResult result(TripShareStorePort.ShareRecord record, String shareToken) {
        List<TripShareStorePort.AccessRecord> events = store.accessEvents(record.id());
        return new ShareLinkResult(
                record.id(),
                record.name(),
                record.scopes(),
                record.passwordHash() != null,
                record.visibility(),
                record.publicNote(),
                record.publicContent(),
                record.expiresAt(),
                record.status(),
                shareToken,
                record.createdBy(),
                record.createdAt(),
                record.updatedAt(),
                events.stream()
                        .filter(TripShareStorePort.AccessRecord::success)
                        .filter(event -> event.reason().startsWith("OPENED"))
                        .count(),
                engagement.countReactions(record.id(), "LIKE"),
                engagement.countReactions(record.id(), "HELPFUL"),
                engagement.countComments(record.id()),
                events.stream()
                        .filter(TripShareStorePort.AccessRecord::success)
                        .filter(event -> event.reason().equals("COPIED_PUBLIC"))
                        .count(),
                record.version());
    }

    private static void version(TripShareStorePort.ShareRecord record, long baseVersion) {
        if (record.version() != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 공유 링크 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", record.version()));
        }
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("SHARED_TRIP_NOT_FOUND", "공유 링크를 찾을 수 없습니다.");
    }
}
