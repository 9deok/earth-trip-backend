package com.earthtrip.wallet.application.service.change;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.wallet.application.port.in.ReservationChangeUseCase;
import com.earthtrip.wallet.application.port.in.ReservationWalletEntryUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.ReservationChangeStorePort;
import com.earthtrip.wallet.application.port.out.ReservationProposalFingerprintPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ReservationChangeService implements ReservationChangeUseCase {

    private static final Set<String> SCHEDULE_FIELDS =
            Set.of("startAt", "endAt", "checkInAt", "checkOutAt", "departureAt", "arrivalAt");
    private static final Set<String> ROUTE_FIELDS =
            Set.of(
                    "placeId",
                    "address",
                    "latitude",
                    "longitude",
                    "departurePlaceId",
                    "arrivalPlaceId");
    private static final Set<String> EXPENSE_FIELDS =
            Set.of("amountMinor", "currency", "priceMinor", "totalMinor");

    private final WalletRecordUseCase records;
    private final ReservationWalletEntryUseCase walletEntries;
    private final ReservationChangeStorePort store;
    private final ReservationProposalFingerprintPort proposalFingerprints;
    private final Clock clock;

    ReservationChangeService(
            WalletRecordUseCase records,
            ReservationWalletEntryUseCase walletEntries,
            ReservationChangeStorePort store,
            ReservationProposalFingerprintPort proposalFingerprints,
            Clock clock) {
        this.records = records;
        this.walletEntries = walletEntries;
        this.store = store;
        this.proposalFingerprints = proposalFingerprints;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(
            UUID tripId, UUID reservationId, UUID actorUserId, ChangeCommand command) {
        WalletRecordUseCase.RecordResult reservation =
                records.get(tripId, actorUserId, "RESERVATION", reservationId);
        requireVersion(reservation.version(), command.reservationBaseVersion());
        validateWalletVersion(tripId, reservationId, actorUserId, command);
        Map<String, Object> proposed =
                command.reservationPayload() == null
                        ? reservation.payload()
                        : command.reservationPayload();
        List<String> changed = changedFields(reservation.payload(), proposed);
        return new PreviewResult(
                proposalHash(tripId, reservationId, command),
                intersects(changed, SCHEDULE_FIELDS),
                intersects(changed, ROUTE_FIELDS),
                intersects(changed, EXPENSE_FIELDS),
                command.walletEntryPayload() != null,
                changed);
    }

    @Override
    public ChangeSetResult apply(
            UUID tripId,
            UUID reservationId,
            UUID actorUserId,
            ChangeCommand command,
            String expectedProposalHash) {
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        ReservationChangeStorePort.ChangeSetRecord existing =
                store.find(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                    || !existing.reservationId().equals(reservationId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 예약 변경에 사용된 요청 ID입니다.");
            }
            return currentResult(existing, actorUserId);
        }
        PreviewResult preview = preview(tripId, reservationId, actorUserId, command);
        if (expectedProposalHash == null
                || !MessageDigest.isEqual(
                        preview.proposalHash().getBytes(StandardCharsets.US_ASCII),
                        expectedProposalHash.getBytes(StandardCharsets.US_ASCII))) {
            throw EarthTripException.conflict(
                    "RESERVATION_PROPOSAL_CHANGED", "미리 본 예약 변경안과 적용할 변경안이 다릅니다.");
        }
        WalletRecordUseCase.RecordResult before =
                records.get(tripId, actorUserId, "RESERVATION", reservationId);
        WalletRecordUseCase.RecordResult beforeWalletEntry =
                currentWalletEntry(tripId, reservationId, actorUserId, false);
        Map<String, Object> payload =
                command.reservationPayload() == null
                        ? before.payload()
                        : Map.copyOf(command.reservationPayload());
        WalletRecordUseCase.RecordResult reservation =
                records.update(
                        tripId,
                        actorUserId,
                        "RESERVATION",
                        reservationId,
                        false,
                        new WalletRecordUseCase.Command(
                                reservationId,
                                null,
                                payload,
                                null,
                                command.visibility(),
                                command.sortOrder(),
                                command.reservationBaseVersion()));
        WalletRecordUseCase.RecordResult walletEntry =
                updateWalletEntry(tripId, reservationId, actorUserId, command);
        Instant now = clock.instant();
        ReservationChangeStorePort.ChangeSetRecord changeSet =
                store.save(
                        new ReservationChangeStorePort.ChangeSetRecord(
                                command.requestId(),
                                tripId,
                                reservationId,
                                actorUserId,
                                preview.proposalHash(),
                                snapshot(before, beforeWalletEntry),
                                snapshot(reservation, walletEntry),
                                now));
        return result(changeSet, reservation, walletEntry);
    }

    private void validateWalletVersion(
            UUID tripId, UUID reservationId, UUID actorUserId, ChangeCommand command) {
        if (command.walletEntryPayload() == null) {
            return;
        }
        WalletRecordUseCase.RecordResult walletEntry =
                currentWalletEntry(tripId, reservationId, actorUserId, false);
        if (walletEntry == null) {
            requireVersion(0, command.walletEntryBaseVersion());
            return;
        }
        requireVersion(walletEntry.version(), command.walletEntryBaseVersion());
    }

    private WalletRecordUseCase.RecordResult updateWalletEntry(
            UUID tripId, UUID reservationId, UUID actorUserId, ChangeCommand command) {
        if (command.walletEntryPayload() == null) {
            return currentWalletEntry(tripId, reservationId, actorUserId, false);
        }
        UUID walletRequestId =
                UUID.nameUUIDFromBytes(
                        ("earthtrip:reservation-wallet-change:" + command.requestId())
                                .getBytes(StandardCharsets.UTF_8));
        return walletEntries.put(
                tripId,
                reservationId,
                actorUserId,
                new ReservationWalletEntryUseCase.Command(
                        walletRequestId,
                        command.walletEntryPayload(),
                        command.visibility(),
                        command.sortOrder(),
                        command.walletEntryBaseVersion()));
    }

    private WalletRecordUseCase.RecordResult currentWalletEntry(
            UUID tripId, UUID reservationId, UUID actorUserId, boolean required) {
        List<WalletRecordUseCase.RecordResult> entries =
                records.list(tripId, actorUserId, "WALLET_ENTRY", reservationId);
        if (entries.size() > 1) {
            throw EarthTripException.conflict(
                    "DUPLICATE_RESERVATION_WALLET_ENTRY", "예약 지갑 항목이 중복되어 있습니다.");
        }
        if (entries.isEmpty()) {
            if (required) {
                throw EarthTripException.notFound(
                        "RESERVATION_WALLET_ENTRY_NOT_FOUND", "예약 지갑 항목을 찾을 수 없습니다.");
            }
            return null;
        }
        return entries.getFirst();
    }

    private ChangeSetResult currentResult(
            ReservationChangeStorePort.ChangeSetRecord changeSet, UUID actorUserId) {
        WalletRecordUseCase.RecordResult reservation =
                records.get(
                        changeSet.tripId(), actorUserId, "RESERVATION", changeSet.reservationId());
        WalletRecordUseCase.RecordResult walletEntry =
                currentWalletEntry(
                        changeSet.tripId(), changeSet.reservationId(), actorUserId, false);
        return result(changeSet, reservation, walletEntry);
    }

    private String proposalHash(UUID tripId, UUID reservationId, ChangeCommand command) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("tripId", tripId.toString());
        value.put("reservationId", reservationId.toString());
        value.put("reservationBaseVersion", command.reservationBaseVersion());
        value.put("walletEntryBaseVersion", command.walletEntryBaseVersion());
        put(value, "reservationPayload", command.reservationPayload());
        put(value, "visibility", command.visibility());
        put(value, "sortOrder", command.sortOrder());
        put(value, "walletEntryPayload", command.walletEntryPayload());
        return proposalFingerprints.fingerprint(value);
    }

    private static List<String> changedFields(
            Map<String, Object> before, Map<String, Object> after) {
        Set<String> keys = new HashSet<>(before.keySet());
        keys.addAll(after.keySet());
        return keys.stream()
                .filter(key -> !java.util.Objects.equals(before.get(key), after.get(key)))
                .sorted()
                .toList();
    }

    private static boolean intersects(List<String> fields, Set<String> target) {
        return fields.stream().anyMatch(target::contains);
    }

    private static Map<String, Object> snapshot(
            WalletRecordUseCase.RecordResult reservation,
            WalletRecordUseCase.RecordResult walletEntry) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reservation", record(reservation));
        if (walletEntry != null) {
            result.put("walletEntry", record(walletEntry));
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> record(WalletRecordUseCase.RecordResult record) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", record.id().toString());
        value.put("payload", record.payload());
        value.put("status", record.status());
        value.put("visibility", record.visibility());
        value.put("sortOrder", record.sortOrder());
        value.put("version", record.version());
        return Map.copyOf(value);
    }

    private static ChangeSetResult result(
            ReservationChangeStorePort.ChangeSetRecord changeSet,
            WalletRecordUseCase.RecordResult reservation,
            WalletRecordUseCase.RecordResult walletEntry) {
        return new ChangeSetResult(
                changeSet.id(),
                changeSet.reservationId(),
                changeSet.proposalHash(),
                reservation,
                walletEntry,
                changeSet.appliedAt());
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 예약 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
