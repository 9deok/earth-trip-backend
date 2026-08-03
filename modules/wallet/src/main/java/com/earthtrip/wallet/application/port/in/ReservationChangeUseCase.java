package com.earthtrip.wallet.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReservationChangeUseCase {

    PreviewResult preview(
        UUID tripId,
        UUID reservationId,
        UUID actorUserId,
        ChangeCommand command
    );

    ChangeSetResult apply(
        UUID tripId,
        UUID reservationId,
        UUID actorUserId,
        ChangeCommand command,
        String expectedProposalHash
    );

    record ChangeCommand(
        UUID requestId,
        Map<String, Object> reservationPayload,
        String visibility,
        Integer sortOrder,
        long reservationBaseVersion,
        Map<String, Object> walletEntryPayload,
        long walletEntryBaseVersion
    ) { }

    record PreviewResult(
        String proposalHash,
        boolean scheduleReviewRequired,
        boolean routeReviewRequired,
        boolean expenseReviewRequired,
        boolean walletRefreshRequired,
        List<String> changedFields
    ) { }

    record ChangeSetResult(
        UUID changeSetId,
        UUID reservationId,
        String proposalHash,
        WalletRecordUseCase.RecordResult reservation,
        WalletRecordUseCase.RecordResult walletEntry,
        Instant appliedAt
    ) { }
}
