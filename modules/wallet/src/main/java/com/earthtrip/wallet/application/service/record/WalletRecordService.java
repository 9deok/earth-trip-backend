package com.earthtrip.wallet.application.service.record;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripChangePublisher;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.WalletRecordStorePort;
import com.earthtrip.wallet.domain.WalletRecord;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class WalletRecordService implements WalletRecordUseCase {
    private static final Set<String> TYPES =
            Set.of(
                    "RESERVATION",
                    "CANCELLATION",
                    "REFUND_RECEIPT",
                    "PREPARATION_TASK",
                    "PACKING_ITEM",
                    "WALLET_ENTRY");
    private static final Set<String> SENSITIVE =
            Set.of("confirmationNumber", "passengerNames", "ticketFileIds", "personalNote");
    private final TripAccess access;
    private final WalletRecordStorePort store;
    private final TripChangePublisher changes;
    private final Clock clock;

    WalletRecordService(TripAccess a, WalletRecordStorePort s, TripChangePublisher p, Clock c) {
        access = a;
        store = s;
        changes = p;
        clock = c;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecordResult> list(UUID trip, UUID actor, String type, UUID parent) {
        type(type);
        TripAccess.AccessResult a = access.requireViewer(trip, actor);
        return store.findAll(trip, type, parent).stream()
                .filter(r -> visible(r, actor))
                .map(r -> result(r, a.role(), actor))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecordResult get(UUID trip, UUID actor, String type, UUID id) {
        type(type);
        TripAccess.AccessResult a = access.requireViewer(trip, actor);
        WalletRecord r = load(trip, type, id);
        if (!visible(r, actor)) throw notFound();
        return result(r, a.role(), actor);
    }

    @Override
    public RecordResult create(UUID trip, UUID actor, String type, boolean memberWrite, Command c) {
        type(type);
        TripAccess.AccessResult a =
                memberWrite ? access.requireViewer(trip, actor) : access.requireEditor(trip, actor);
        WalletRecord old = store.findById(c.requestId()).orElse(null);
        if (old != null) {
            if (!old.tripId().equals(trip)
                    || !old.type().equals(type)
                    || !old.createdBy().equals(actor))
                throw EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
            return result(old, a.role(), actor);
        }
        WalletRecord r =
                WalletRecord.create(
                        c.requestId(),
                        trip,
                        type,
                        c.parentId(),
                        payload(c.payload()),
                        c.status() == null ? "ACTIVE" : c.status(),
                        c.visibility() == null ? "TRIP" : c.visibility(),
                        c.sortOrder() == null ? 0 : c.sortOrder(),
                        actor,
                        clock.instant());
        WalletRecord saved = store.save(r);
        changes.publish(trip, actor, "CREATED", type, saved.id());
        return result(saved, a.role(), actor);
    }

    @Override
    public RecordResult update(
            UUID trip, UUID actor, String type, UUID id, boolean memberWrite, Command c) {
        type(type);
        TripAccess.AccessResult a =
                memberWrite ? access.requireViewer(trip, actor) : access.requireEditor(trip, actor);
        WalletRecord r = load(trip, type, id);
        requireVisible(r, actor);
        if (memberWrite && !r.createdBy().equals(actor))
            throw EarthTripException.forbidden("RECORD_AUTHOR_REQUIRED", "작성자만 변경할 수 있습니다.");
        version(r, c.baseVersion());
        r.update(
                c.payload() == null ? null : payload(c.payload()),
                c.status(),
                c.visibility(),
                c.sortOrder(),
                actor,
                clock.instant());
        WalletRecord saved = store.save(r);
        changes.publish(trip, actor, "UPDATED", type, id);
        return result(saved, a.role(), actor);
    }

    @Override
    public void delete(UUID trip, UUID actor, String type, UUID id, boolean memberWrite, long v) {
        type(type);
        if (memberWrite) access.requireViewer(trip, actor);
        else access.requireEditor(trip, actor);
        WalletRecord r = load(trip, type, id);
        requireVisible(r, actor);
        if (memberWrite && !r.createdBy().equals(actor))
            throw EarthTripException.forbidden("RECORD_AUTHOR_REQUIRED", "작성자만 삭제할 수 있습니다.");
        version(r, v);
        r.delete(actor, clock.instant());
        store.save(r);
        changes.publish(trip, actor, "DELETED", type, id);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummary wallet(UUID trip, UUID actor) {
        List<RecordResult> reservations = list(trip, actor, "RESERVATION", null);
        List<RecordResult> entries = list(trip, actor, "WALLET_ENTRY", null);
        int ready =
                (int)
                        entries.stream()
                                .filter(e -> Boolean.TRUE.equals(e.payload().get("offlineReady")))
                                .count();
        int action =
                (int)
                        entries.stream()
                                .filter(e -> Boolean.TRUE.equals(e.payload().get("actionRequired")))
                                .count();
        return new WalletSummary(reservations, entries, ready, action);
    }

    private WalletRecord load(UUID trip, String type, UUID id) {
        return store.findById(id)
                .filter(r -> r.tripId().equals(trip) && r.type().equals(type))
                .orElseThrow(WalletRecordService::notFound);
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("WALLET_RECORD_NOT_FOUND", "여행 지갑 항목을 찾을 수 없습니다.");
    }

    private static boolean visible(WalletRecord r, UUID actor) {
        if (r.visibility().equals("PRIVATE")) return r.createdBy().equals(actor);
        if (r.visibility().equals("PARTICIPANTS")) {
            Object ids = r.payload().get("participantIds");
            return r.createdBy().equals(actor)
                    || (ids instanceof Collection<?> c
                            && c.stream().map(String::valueOf).anyMatch(actor.toString()::equals));
        }
        return true;
    }

    private static void requireVisible(WalletRecord record, UUID actor) {
        if (!visible(record, actor)) throw notFound();
    }

    private static RecordResult result(WalletRecord r, String role, UUID actor) {
        Map<String, Object> data = new LinkedHashMap<>(r.payload());
        if (role.equals("VIEWER") && !r.createdBy().equals(actor)) SENSITIVE.forEach(data::remove);
        return new RecordResult(
                r.id(),
                r.tripId(),
                r.type(),
                r.parentId(),
                Map.copyOf(data),
                r.status(),
                r.visibility(),
                r.sortOrder(),
                r.version(),
                r.createdBy(),
                r.updatedBy(),
                r.createdAt(),
                r.updatedAt());
    }

    private static Map<String, Object> payload(Map<String, Object> p) {
        if (p == null) throw EarthTripException.badRequest("PAYLOAD_REQUIRED", "payload가 필요합니다.");
        return new LinkedHashMap<>(p);
    }

    private static void type(String t) {
        if (!TYPES.contains(t))
            throw EarthTripException.badRequest(
                    "INVALID_WALLET_RECORD_TYPE", "지원하지 않는 지갑 항목 유형입니다.");
    }

    private static void version(WalletRecord r, long v) {
        if (r.version() != v)
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 지갑 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", r.version()));
    }
}
