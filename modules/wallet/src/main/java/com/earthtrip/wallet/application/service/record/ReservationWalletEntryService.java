package com.earthtrip.wallet.application.service.record;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.wallet.application.port.in.ReservationWalletEntryUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ReservationWalletEntryService implements ReservationWalletEntryUseCase {

    private final WalletRecordUseCase records;

    ReservationWalletEntryService(WalletRecordUseCase records) {
        this.records = records;
    }

    @Override
    public WalletRecordUseCase.RecordResult put(
            UUID tripId, UUID reservationId, UUID actorUserId, Command command) {
        records.get(tripId, actorUserId, "RESERVATION", reservationId);
        List<WalletRecordUseCase.RecordResult> entries =
                records.list(tripId, actorUserId, "WALLET_ENTRY", reservationId);
        if (entries.size() > 1) {
            throw EarthTripException.conflict(
                    "DUPLICATE_RESERVATION_WALLET_ENTRY", "예약에 여행 지갑 항목이 여러 개 연결되어 관리자 확인이 필요합니다.");
        }
        Map<String, Object> payload = command.payload();
        if (payload == null) {
            throw EarthTripException.badRequest("PAYLOAD_REQUIRED", "여행 지갑에 표시할 정보가 필요합니다.");
        }
        if (entries.isEmpty()) {
            if (command.requestId() == null) {
                throw EarthTripException.badRequest(
                        "REQUEST_ID_REQUIRED", "새 여행 지갑 항목의 요청 ID가 필요합니다.");
            }
            return records.create(
                    tripId,
                    actorUserId,
                    "WALLET_ENTRY",
                    false,
                    new WalletRecordUseCase.Command(
                            command.requestId(),
                            reservationId,
                            payload,
                            "ACTIVE",
                            command.visibility(),
                            command.sortOrder(),
                            0));
        }
        WalletRecordUseCase.RecordResult current = entries.getFirst();
        return records.update(
                tripId,
                actorUserId,
                "WALLET_ENTRY",
                current.id(),
                false,
                new WalletRecordUseCase.Command(
                        current.id(),
                        reservationId,
                        payload,
                        "ACTIVE",
                        command.visibility(),
                        command.sortOrder(),
                        command.baseVersion()));
    }

    @Override
    public void delete(UUID tripId, UUID reservationId, UUID actorUserId, long baseVersion) {
        records.get(tripId, actorUserId, "RESERVATION", reservationId);
        List<WalletRecordUseCase.RecordResult> entries =
                records.list(tripId, actorUserId, "WALLET_ENTRY", reservationId);
        if (entries.isEmpty()) {
            return;
        }
        if (entries.size() > 1) {
            throw EarthTripException.conflict(
                    "DUPLICATE_RESERVATION_WALLET_ENTRY", "예약에 여행 지갑 항목이 여러 개 연결되어 관리자 확인이 필요합니다.");
        }
        records.delete(
                tripId, actorUserId, "WALLET_ENTRY", entries.getFirst().id(), false, baseVersion);
    }
}
