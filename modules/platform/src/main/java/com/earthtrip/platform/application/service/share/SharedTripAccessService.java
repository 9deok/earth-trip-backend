package com.earthtrip.platform.application.service.share;

import static com.earthtrip.platform.application.service.share.TripSharePolicy.*;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.SharedTripAccessUseCase;
import com.earthtrip.platform.application.port.out.ShareCredentialPort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SharedTripAccessService implements SharedTripAccessUseCase {

    private final TripStructureView structure;
    private final TripPlanningView planning;
    private final TripWalletView wallet;
    private final TripExpenseView expenses;
    private final TripAccess tripAccess;
    private final TripShareStorePort store;
    private final ShareAccessRecorder accessRecorder;
    private final ShareCredentialPort credentials;
    private final Clock clock;

    SharedTripAccessService(
            TripStructureView structure,
            TripPlanningView planning,
            TripWalletView wallet,
            TripExpenseView expenses,
            TripAccess tripAccess,
            TripShareStorePort store,
            ShareAccessRecorder accessRecorder,
            ShareCredentialPort credentials,
            Clock clock) {
        this.structure = structure;
        this.planning = planning;
        this.wallet = wallet;
        this.expenses = expenses;
        this.tripAccess = tripAccess;
        this.store = store;
        this.accessRecorder = accessRecorder;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    public SharedTripResult sharedTrip(String token, String passwordSessionToken) {
        TripShareStorePort.ShareRecord share = loadToken(token);
        requireActive(share);
        requireTripAvailable(share);
        if (share.passwordHash() != null && !validSession(share, passwordSessionToken)) {
            event(share.id(), false, "PASSWORD_REQUIRED");
            throw new EarthTripException("SHARE_PASSWORD_REQUIRED", 401, "공유 링크 비밀번호 확인이 필요합니다.");
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
        requireTripAvailable(share);
        if (share.passwordHash() == null
                || !credentials.matchesPassword(password, share.passwordHash())) {
            event(share.id(), false, "PASSWORD_FAILED");
            throw EarthTripException.unauthorized(
                    "INVALID_SHARE_PASSWORD", "공유 링크 비밀번호가 올바르지 않습니다.");
        }
        String sessionToken = credentials.newToken();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(PASSWORD_SESSION_TTL);
        store.savePasswordSession(
                new TripShareStorePort.PasswordSessionRecord(
                        credentials.hashToken(sessionToken), share.id(), expiresAt, now));
        event(share.id(), true, "PASSWORD_VERIFIED");
        return new PasswordSessionResult(sessionToken, expiresAt);
    }

    private SharedTripResult shared(TripShareStorePort.ShareRecord share) {
        TripStructureView.StructureSnapshot trip =
                structure.snapshot(share.tripId(), share.projectionUserId());
        List<SharedSegment> segments =
                share.scopes().contains("STRUCTURE")
                        ? trip.segments().stream()
                                .map(
                                        segment ->
                                                new SharedSegment(
                                                        segment.type(),
                                                        segment.cityName(),
                                                        segment.countryCode(),
                                                        segment.startDate(),
                                                        segment.endDate(),
                                                        segment.accommodationName(),
                                                        segment.transportMode(),
                                                        segment.sortOrder()))
                                .toList()
                        : List.of();
        List<SharedPlanningItem> items =
                share.scopes().contains("ITINERARY")
                        ? planning.searchEntries(share.tripId(), share.projectionUserId()).stream()
                                .filter(entry -> entry.type().equals("SCHEDULE_ITEM"))
                                .map(
                                        entry ->
                                                new SharedPlanningItem(
                                                        entry.localDate(),
                                                        title(entry.payload()),
                                                        entry.status(),
                                                        entry.sortOrder(),
                                                        allow(
                                                                entry.payload(),
                                                                PUBLIC_PLANNING_FIELDS)))
                                .toList()
                        : List.of();
        List<Map<String, Object>> reservations =
                share.scopes().contains("RESERVATIONS")
                        ? wallet
                                .snapshot(share.tripId(), share.projectionUserId())
                                .reservations()
                                .stream()
                                .map(entry -> allow(entry.payload(), PUBLIC_RESERVATION_FIELDS))
                                .toList()
                        : List.of();
        List<SharedTotal> totals =
                share.scopes().contains("BUDGET_SUMMARY")
                        ? expenses
                                .summary(share.tripId(), share.projectionUserId())
                                .totals()
                                .stream()
                                .map(
                                        total ->
                                                new SharedTotal(
                                                        total.currency(), total.amountMinor()))
                                .toList()
                        : List.of();
        return new SharedTripResult(
                trip.trip().title(),
                trip.trip().startDate(),
                trip.trip().endDate(),
                trip.trip().timeZone(),
                share.scopes(),
                segments,
                items,
                reservations,
                totals);
    }

    private boolean validSession(TripShareStorePort.ShareRecord share, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return store.findPasswordSession(credentials.hashToken(token))
                .filter(session -> session.shareId().equals(share.id()))
                .filter(session -> session.expiresAt().isAfter(clock.instant()))
                .isPresent();
    }

    private TripShareStorePort.ShareRecord loadToken(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            throw notFound();
        }
        return store.findByTokenHash(credentials.hashToken(token))
                .orElseThrow(SharedTripAccessService::notFound);
    }

    private void requireActive(TripShareStorePort.ShareRecord share) {
        Instant now = clock.instant();
        if (!share.status().equals("ACTIVE")
                || share.expiresAt() != null && !share.expiresAt().isAfter(now)) {
            event(share.id(), false, "EXPIRED_OR_REVOKED");
            throw notFound();
        }
    }

    private void requireTripAvailable(TripShareStorePort.ShareRecord share) {
        try {
            tripAccess.publicInfo(share.tripId());
        } catch (EarthTripException exception) {
            event(share.id(), false, "TRIP_UNAVAILABLE");
            throw notFound();
        }
    }

    private void event(java.util.UUID shareId, boolean success, String reason) {
        accessRecorder.record(shareId, success, reason);
    }

    private static Map<String, Object> allow(Map<String, Object> payload, Set<String> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        fields.stream()
                .sorted()
                .forEach(
                        field -> {
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

    private static EarthTripException notFound() {
        return EarthTripException.notFound("SHARED_TRIP_NOT_FOUND", "공유 링크를 찾을 수 없습니다.");
    }
}
