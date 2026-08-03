package com.earthtrip.platform.application.service.share;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.TripShareUseCase;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripShareService implements TripShareUseCase {

    private static final Set<String> SCOPES = Set.of(
        "STRUCTURE", "ITINERARY", "RESERVATIONS", "BUDGET_SUMMARY"
    );
    private static final Set<String> PUBLIC_RESERVATION_FIELDS = Set.of(
        "title", "name", "provider", "type", "category", "startAt", "endAt",
        "address", "status"
    );
    private static final Set<String> PUBLIC_PLANNING_FIELDS = Set.of(
        "title", "name", "startMinute", "endMinute", "address", "latitude",
        "longitude", "fixedTime"
    );
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final Duration PASSWORD_SESSION_TTL = Duration.ofMinutes(15);

    private final TripAccess access;
    private final TripStructureView structure;
    private final TripPlanningView planning;
    private final TripWalletView wallet;
    private final TripExpenseView expenses;
    private final TripShareStorePort store;
    private final ShareAccessRecorder accessRecorder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    TripShareService(
        TripAccess access,
        TripStructureView structure,
        TripPlanningView planning,
        TripWalletView wallet,
        TripExpenseView expenses,
        TripShareStorePort store,
        ShareAccessRecorder accessRecorder,
        Clock clock
    ) {
        this.access = access;
        this.structure = structure;
        this.planning = planning;
        this.wallet = wallet;
        this.expenses = expenses;
        this.store = store;
        this.accessRecorder = accessRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkResult> list(UUID tripId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        return store.findAll(tripId).stream().map(record -> result(record, null)).toList();
    }

    @Override
    public ShareLinkResult create(
        UUID tripId,
        UUID actorUserId,
        ShareLinkCommand command
    ) {
        TripAccess.AccessResult tripAccess = access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        TripShareStorePort.ShareRecord existing = store.findById(command.requestId())
            .orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "이미 다른 공유 링크에 사용된 요청 ID입니다."
                );
            }
            return result(existing, null);
        }
        String token = token();
        Instant now = clock.instant();
        TripShareStorePort.ShareRecord saved = store.save(new TripShareStorePort.ShareRecord(
            command.requestId(), tripId, hashToken(token), name(command.name()),
            scopes(command.scopes()), password(command.password()), tripAccess.ownerUserId(),
            expiry(command.expiresAt(), now), "ACTIVE", actorUserId, now, now, null, 0
        ));
        return result(saved, token);
    }

    @Override
    public ShareLinkResult update(
        UUID tripId,
        UUID shareId,
        UUID actorUserId,
        ShareLinkCommand command
    ) {
        access.requireEditor(tripId, actorUserId);
        TripShareStorePort.ShareRecord current = load(tripId, shareId);
        version(current, command.baseVersion());
        if (!current.status().equals("ACTIVE")) {
            throw EarthTripException.conflict(
                "SHARE_LINK_REVOKED", "회수된 공유 링크는 수정할 수 없습니다."
            );
        }
        String passwordHash = Boolean.TRUE.equals(command.removePassword())
            ? null
            : command.password() == null
                ? current.passwordHash()
                : password(command.password());
        Instant now = clock.instant();
        TripShareStorePort.ShareRecord saved = store.save(new TripShareStorePort.ShareRecord(
            current.id(), tripId, current.tokenHash(),
            command.name() == null ? current.name() : name(command.name()),
            command.scopes() == null ? current.scopes() : scopes(command.scopes()),
            passwordHash, current.projectionUserId(),
            Boolean.TRUE.equals(command.removeExpiry())
                ? null
                : command.expiresAt() == null
                    ? current.expiresAt()
                    : expiry(command.expiresAt(), now),
            current.status(), current.createdBy(), current.createdAt(), now,
            current.revokedAt(), current.version()
        ));
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
        store.save(new TripShareStorePort.ShareRecord(
            current.id(), tripId, current.tokenHash(), current.name(), current.scopes(),
            current.passwordHash(), current.projectionUserId(), current.expiresAt(),
            "REVOKED", current.createdBy(), current.createdAt(), now, now, current.version()
        ));
    }

    @Override
    public SharedTripResult sharedTrip(String token, String passwordSessionToken) {
        TripShareStorePort.ShareRecord share = loadToken(token);
        requireActive(share);
        if (share.passwordHash() != null && !validSession(share, passwordSessionToken)) {
            event(share.id(), false, "PASSWORD_REQUIRED");
            throw new EarthTripException(
                "SHARE_PASSWORD_REQUIRED", 401, "공유 링크 비밀번호 확인이 필요합니다."
            );
        }
        try {
            SharedTripResult result = shared(share);
            event(share.id(), true, "OPENED");
            return result;
        } catch (EarthTripException exception) {
            event(share.id(), false, "PROJECTION_DENIED");
            throw exception;
        }
    }

    @Override
    public PasswordSessionResult verifyPassword(String token, String password) {
        TripShareStorePort.ShareRecord share = loadToken(token);
        requireActive(share);
        if (share.passwordHash() == null || !passwordMatches(password, share.passwordHash())) {
            event(share.id(), false, "PASSWORD_FAILED");
            throw EarthTripException.unauthorized(
                "INVALID_SHARE_PASSWORD", "공유 링크 비밀번호가 올바르지 않습니다."
            );
        }
        String sessionToken = token();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(PASSWORD_SESSION_TTL);
        store.savePasswordSession(new TripShareStorePort.PasswordSessionRecord(
            hashToken(sessionToken), share.id(), expiresAt, now
        ));
        event(share.id(), true, "PASSWORD_VERIFIED");
        return new PasswordSessionResult(sessionToken, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessEventResult> accessEvents(
        UUID tripId,
        UUID shareId,
        UUID actorUserId
    ) {
        access.requireEditor(tripId, actorUserId);
        load(tripId, shareId);
        return store.accessEvents(shareId).stream().limit(1_000)
            .map(record -> new AccessEventResult(
                record.eventId(), record.success(), record.reason(), record.occurredAt()
            ))
            .toList();
    }

    private SharedTripResult shared(TripShareStorePort.ShareRecord share) {
        TripStructureView.StructureSnapshot trip = structure.snapshot(
            share.tripId(), share.projectionUserId()
        );
        List<SharedSegment> segments = share.scopes().contains("STRUCTURE")
            ? trip.segments().stream().map(segment -> new SharedSegment(
                segment.type(), segment.cityName(), segment.countryCode(),
                segment.startDate(), segment.endDate(), segment.accommodationName(),
                segment.transportMode(), segment.sortOrder()
            )).toList()
            : List.of();
        List<SharedPlanningItem> items = share.scopes().contains("ITINERARY")
            ? planning.searchEntries(share.tripId(), share.projectionUserId()).stream()
                .filter(entry -> entry.type().equals("SCHEDULE_ITEM"))
                .map(entry -> new SharedPlanningItem(
                    entry.localDate(), title(entry.payload()), entry.status(),
                    entry.sortOrder(), allow(entry.payload(), PUBLIC_PLANNING_FIELDS)
                ))
                .toList()
            : List.of();
        List<Map<String, Object>> reservations = share.scopes().contains("RESERVATIONS")
            ? wallet.snapshot(share.tripId(), share.projectionUserId()).reservations().stream()
                .map(entry -> allow(entry.payload(), PUBLIC_RESERVATION_FIELDS))
                .toList()
            : List.of();
        List<SharedTotal> totals = share.scopes().contains("BUDGET_SUMMARY")
            ? expenses.summary(share.tripId(), share.projectionUserId()).totals().stream()
                .map(total -> new SharedTotal(total.currency(), total.amountMinor()))
                .toList()
            : List.of();
        return new SharedTripResult(
            trip.trip().title(), trip.trip().startDate(), trip.trip().endDate(),
            trip.trip().timeZone(), share.scopes(), segments, items, reservations, totals
        );
    }

    private boolean validSession(TripShareStorePort.ShareRecord share, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return store.findPasswordSession(hashToken(token))
            .filter(session -> session.shareId().equals(share.id()))
            .filter(session -> session.expiresAt().isAfter(clock.instant()))
            .isPresent();
    }

    private TripShareStorePort.ShareRecord load(UUID tripId, UUID shareId) {
        return store.findById(shareId)
            .filter(share -> share.tripId().equals(tripId))
            .orElseThrow(TripShareService::notFound);
    }

    private TripShareStorePort.ShareRecord loadToken(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            throw notFound();
        }
        return store.findByTokenHash(hashToken(token)).orElseThrow(TripShareService::notFound);
    }

    private void requireActive(TripShareStorePort.ShareRecord share) {
        Instant now = clock.instant();
        if (!share.status().equals("ACTIVE")
            || share.expiresAt() != null && !share.expiresAt().isAfter(now)) {
            event(share.id(), false, "EXPIRED_OR_REVOKED");
            throw notFound();
        }
    }

    private void event(UUID shareId, boolean success, String reason) {
        accessRecorder.record(shareId, success, reason);
    }

    private static Map<String, Object> allow(
        Map<String, Object> payload,
        Set<String> fields
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        fields.stream().sorted().forEach(field -> {
            Object value = payload.get(field);
            if (value != null) {
                result.put(field, value);
            }
        });
        return Map.copyOf(result);
    }

    private static String title(Map<String, Object> payload) {
        for (String key : List.of("title", "name", "placeName")) {
            if (payload.get(key) != null) {
                return String.valueOf(payload.get(key));
            }
        }
        return "일정";
    }

    private static String name(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw EarthTripException.badRequest(
                "INVALID_SHARE_NAME", "공유 링크 이름은 1~120자여야 합니다."
            );
        }
        return value.strip();
    }

    private static List<String> scopes(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw EarthTripException.badRequest(
                "SHARE_SCOPE_REQUIRED", "공유할 범위를 하나 이상 선택해 주세요."
            );
        }
        List<String> normalized = values.stream()
            .map(value -> value.strip().toUpperCase(Locale.ROOT)).distinct().sorted().toList();
        if (!SCOPES.containsAll(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_SHARE_SCOPE", "지원하지 않는 공유 범위가 포함되어 있습니다."
            );
        }
        return normalized;
    }

    private static Instant expiry(Instant value, Instant now) {
        if (value != null && !value.isAfter(now)) {
            throw EarthTripException.badRequest(
                "INVALID_SHARE_EXPIRY", "공유 링크 만료 시각은 현재보다 뒤여야 합니다."
            );
        }
        return value;
    }

    private static String password(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() < 4 || value.length() > 128) {
            throw EarthTripException.badRequest(
                "INVALID_SHARE_PASSWORD", "공유 비밀번호는 4~128자여야 합니다."
            );
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(value.toCharArray(), salt, PASSWORD_ITERATIONS);
        return "pbkdf2-sha256$" + PASSWORD_ITERATIONS + "$"
            + Base64.getEncoder().encodeToString(salt) + "$"
            + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean passwordMatches(String value, String encoded) {
        if (value == null) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(value.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("저장된 공유 비밀번호 해시가 올바르지 않습니다.", exception);
        }
    }

    private static byte[] pbkdf2(char[] value, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(value, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("공유 비밀번호를 안전하게 처리할 수 없습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static ShareLinkResult result(
        TripShareStorePort.ShareRecord record,
        String shareToken
    ) {
        return new ShareLinkResult(
            record.id(), record.name(), record.scopes(), record.passwordHash() != null,
            record.expiresAt(), record.status(), shareToken, record.createdBy(),
            record.createdAt(), record.updatedAt(), record.version()
        );
    }

    private static void version(TripShareStorePort.ShareRecord record, long baseVersion) {
        if (record.version() != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 공유 링크 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", record.version())
            );
        }
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound(
            "SHARED_TRIP_NOT_FOUND", "공유 링크를 찾을 수 없습니다."
        );
    }
}
