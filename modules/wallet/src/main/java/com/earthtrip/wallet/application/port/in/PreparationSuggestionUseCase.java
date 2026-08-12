package com.earthtrip.wallet.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PreparationSuggestionUseCase {

    List<SuggestionResult> list(UUID tripId, UUID actorUserId);

    AcceptanceResult accept(UUID tripId, UUID suggestionId, UUID actorUserId, UUID requestId);

    void dismiss(UUID tripId, UUID suggestionId, UUID actorUserId, String reason);

    record SuggestionResult(
            UUID suggestionId,
            String ruleCode,
            String targetType,
            String title,
            String reason,
            Map<String, Object> payload) {}

    record AcceptanceResult(UUID suggestionId, WalletRecordUseCase.RecordResult createdRecord) {}
}
