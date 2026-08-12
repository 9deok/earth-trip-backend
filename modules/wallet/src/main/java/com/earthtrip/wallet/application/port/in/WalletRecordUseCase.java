package com.earthtrip.wallet.application.port.in;

import java.time.Instant;
import java.util.*;

public interface WalletRecordUseCase {
    List<RecordResult> list(UUID trip, UUID actor, String type, UUID parent);

    RecordResult get(UUID trip, UUID actor, String type, UUID id);

    RecordResult create(UUID trip, UUID actor, String type, boolean memberWrite, Command c);

    RecordResult update(
            UUID trip, UUID actor, String type, UUID id, boolean memberWrite, Command c);

    void delete(UUID trip, UUID actor, String type, UUID id, boolean memberWrite, long baseVersion);

    WalletSummary wallet(UUID trip, UUID actor);

    record Command(
            UUID requestId,
            UUID parentId,
            Map<String, Object> payload,
            String status,
            String visibility,
            Integer sortOrder,
            long baseVersion) {}

    record RecordResult(
            UUID id,
            UUID tripId,
            String type,
            UUID parentId,
            Map<String, Object> payload,
            String status,
            String visibility,
            int sortOrder,
            long version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {}

    record WalletSummary(
            List<RecordResult> reservations,
            List<RecordResult> tickets,
            int offlineReadyCount,
            int actionRequiredCount) {}
}
