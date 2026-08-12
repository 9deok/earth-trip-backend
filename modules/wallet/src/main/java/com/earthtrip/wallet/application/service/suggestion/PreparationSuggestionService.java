package com.earthtrip.wallet.application.service.suggestion;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.wallet.application.port.in.PreparationSuggestionUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PreparationSuggestionService implements PreparationSuggestionUseCase {

    private final WalletRecordUseCase records;
    private final PackingTemplateStorePort store;
    private final Clock clock;

    PreparationSuggestionService(
            WalletRecordUseCase records, PackingTemplateStorePort store, Clock clock) {
        this.records = records;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResult> list(UUID tripId, UUID actorUserId) {
        return generated(tripId, actorUserId).stream()
                .filter(item -> !store.isDismissed(tripId, actorUserId, item.suggestionId()))
                .filter(item -> !accepted(tripId, actorUserId, item.suggestionId()))
                .toList();
    }

    @Override
    public AcceptanceResult accept(
            UUID tripId, UUID suggestionId, UUID actorUserId, UUID requestId) {
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        SuggestionResult suggestion = requireSuggestion(tripId, suggestionId, actorUserId);
        Map<String, Object> payload = new LinkedHashMap<>(suggestion.payload());
        payload.put("title", suggestion.title());
        payload.put("suggestionId", suggestionId.toString());
        payload.put("suggestionRule", suggestion.ruleCode());
        WalletRecordUseCase.RecordResult created =
                records.create(
                        tripId,
                        actorUserId,
                        suggestion.targetType(),
                        false,
                        new WalletRecordUseCase.Command(
                                requestId, null, payload, "OPEN", "TRIP", null, 0));
        return new AcceptanceResult(suggestionId, created);
    }

    @Override
    public void dismiss(UUID tripId, UUID suggestionId, UUID actorUserId, String reason) {
        requireSuggestion(tripId, suggestionId, actorUserId);
        String normalized = reason == null || reason.isBlank() ? null : reason.strip();
        if (normalized != null && normalized.length() > 500) {
            throw EarthTripException.badRequest(
                    "SUGGESTION_DISMISSAL_REASON_TOO_LONG", "숨김 사유는 500자 이하여야 합니다.");
        }
        store.saveDismissal(
                new PackingTemplateStorePort.DismissalRecord(
                        suggestionId, tripId, actorUserId, normalized, clock.instant()));
    }

    private SuggestionResult requireSuggestion(UUID tripId, UUID suggestionId, UUID actorUserId) {
        if (store.isDismissed(tripId, actorUserId, suggestionId)) {
            throw EarthTripException.notFound(
                    "PREPARATION_SUGGESTION_NOT_FOUND", "준비 제안을 찾을 수 없습니다.");
        }
        return generated(tripId, actorUserId).stream()
                .filter(item -> item.suggestionId().equals(suggestionId))
                .findFirst()
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "PREPARATION_SUGGESTION_NOT_FOUND", "준비 제안을 찾을 수 없습니다."));
    }

    private List<SuggestionResult> generated(UUID tripId, UUID actorUserId) {
        List<WalletRecordUseCase.RecordResult> reservations =
                records.list(tripId, actorUserId, "RESERVATION", null);
        List<WalletRecordUseCase.RecordResult> entries =
                records.list(tripId, actorUserId, "WALLET_ENTRY", null);
        Set<UUID> linked =
                entries.stream()
                        .map(WalletRecordUseCase.RecordResult::parentId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
        List<SuggestionResult> suggestions = new ArrayList<>();
        for (WalletRecordUseCase.RecordResult reservation : reservations) {
            if (reservation.status().equals("CONFIRMED") && !linked.contains(reservation.id())) {
                suggestions.add(
                        suggestion(
                                tripId,
                                "ADD_RESERVATION_TO_WALLET",
                                reservation.id(),
                                "PREPARATION_TASK",
                                "예약을 여행 지갑에 저장하기",
                                "확정 예약을 현장에서 빠르게 찾을 수 있도록 준비합니다.",
                                Map.of("reservationId", reservation.id().toString())));
            }
            if (isFlight(reservation.payload())) {
                suggestions.add(
                        suggestion(
                                tripId,
                                "PACK_PASSPORT",
                                reservation.id(),
                                "PACKING_ITEM",
                                "여권 챙기기",
                                "항공 예약이 있어 여권 준비가 필요합니다.",
                                Map.of("name", "여권", "category", "DOCUMENT", "quantity", 1)));
            }
        }
        for (WalletRecordUseCase.RecordResult entry : entries) {
            if (Boolean.TRUE.equals(entry.payload().get("actionRequired"))) {
                suggestions.add(
                        suggestion(
                                tripId,
                                "COMPLETE_WALLET_ACTION",
                                entry.id(),
                                "PREPARATION_TASK",
                                "지갑 항목 확인 완료하기",
                                "예약 제공자가 추가 확인을 요구합니다.",
                                Map.of("walletEntryId", entry.id().toString())));
            }
            if (!Boolean.TRUE.equals(entry.payload().get("offlineReady"))) {
                suggestions.add(
                        suggestion(
                                tripId,
                                "PREPARE_OFFLINE_TICKET",
                                entry.id(),
                                "PREPARATION_TASK",
                                "티켓을 오프라인에서도 열 수 있게 준비하기",
                                "연결이 불안정한 현장에서도 예약 정보를 확인할 수 있게 합니다.",
                                Map.of("walletEntryId", entry.id().toString())));
            }
        }
        return suggestions.stream().distinct().toList();
    }

    private boolean accepted(UUID tripId, UUID actorUserId, UUID suggestionId) {
        return List.of("PREPARATION_TASK", "PACKING_ITEM").stream()
                .flatMap(type -> records.list(tripId, actorUserId, type, null).stream())
                .anyMatch(
                        record ->
                                suggestionId
                                        .toString()
                                        .equals(
                                                String.valueOf(
                                                        record.payload().get("suggestionId"))));
    }

    private static boolean isFlight(Map<String, Object> payload) {
        for (String key : List.of("type", "category", "reservationType")) {
            Object value = payload.get(key);
            if (value != null
                    && String.valueOf(value).strip().toUpperCase(Locale.ROOT).contains("FLIGHT")) {
                return true;
            }
        }
        return false;
    }

    private static SuggestionResult suggestion(
            UUID tripId,
            String rule,
            UUID sourceId,
            String targetType,
            String title,
            String reason,
            Map<String, Object> payload) {
        UUID id =
                UUID.nameUUIDFromBytes(
                        (tripId + "|" + rule + "|" + sourceId).getBytes(StandardCharsets.UTF_8));
        return new SuggestionResult(id, rule, targetType, title, reason, payload);
    }
}
