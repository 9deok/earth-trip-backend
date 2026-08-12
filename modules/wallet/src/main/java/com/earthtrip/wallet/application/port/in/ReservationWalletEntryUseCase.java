package com.earthtrip.wallet.application.port.in;

import java.util.Map;
import java.util.UUID;

public interface ReservationWalletEntryUseCase {

    WalletRecordUseCase.RecordResult put(
            UUID tripId, UUID reservationId, UUID actorUserId, Command command);

    void delete(UUID tripId, UUID reservationId, UUID actorUserId, long baseVersion);

    record Command(
            UUID requestId,
            Map<String, Object> payload,
            String visibility,
            Integer sortOrder,
            long baseVersion) {}
}
