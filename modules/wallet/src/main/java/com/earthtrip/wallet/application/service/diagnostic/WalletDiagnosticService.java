package com.earthtrip.wallet.application.service.diagnostic;

import com.earthtrip.wallet.application.port.in.WalletDiagnosticUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class WalletDiagnosticService implements WalletDiagnosticUseCase {

    private final WalletRecordUseCase records;

    WalletDiagnosticService(WalletRecordUseCase records) {
        this.records = records;
    }

    @Override
    public List<DiagnosticResult> list(UUID tripId, UUID actorUserId) {
        List<WalletRecordUseCase.RecordResult> reservations =
                records.list(tripId, actorUserId, "RESERVATION", null);
        List<WalletRecordUseCase.RecordResult> entries =
                records.list(tripId, actorUserId, "WALLET_ENTRY", null);
        Set<UUID> linkedReservations =
                entries.stream()
                        .map(WalletRecordUseCase.RecordResult::parentId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
        List<DiagnosticResult> diagnostics = new ArrayList<>();
        for (WalletRecordUseCase.RecordResult reservation : reservations) {
            if (reservation.status().equals("CONFIRMED")
                    && !linkedReservations.contains(reservation.id())) {
                diagnostics.add(
                        result(
                                tripId,
                                "MISSING_WALLET_ENTRY",
                                "WARNING",
                                reservation.id(),
                                "확정 예약이 여행 지갑에 저장되지 않았습니다.",
                                Map.of()));
            }
        }
        for (WalletRecordUseCase.RecordResult entry : entries) {
            addEntryDiagnostics(diagnostics, tripId, entry);
        }
        return List.copyOf(diagnostics);
    }

    private static void addEntryDiagnostics(
            List<DiagnosticResult> diagnostics,
            UUID tripId,
            WalletRecordUseCase.RecordResult entry) {
        Map<String, Object> payload = entry.payload();
        if (Boolean.TRUE.equals(payload.get("ticketRequired"))
                && empty(payload.get("ticketFileIds"))) {
            diagnostics.add(
                    result(
                            tripId,
                            "TICKET_FILE_MISSING",
                            "ERROR",
                            entry.id(),
                            "필요한 티켓 파일이 연결되지 않았습니다.",
                            Map.of()));
        }
        if (Boolean.TRUE.equals(payload.get("requiresAppLogin"))) {
            diagnostics.add(
                    result(
                            tripId,
                            "APP_LOGIN_REQUIRED",
                            "WARNING",
                            entry.id(),
                            "현장에서 외부 앱 로그인이 필요한 항목입니다.",
                            Map.of()));
        }
        if (Boolean.TRUE.equals(payload.get("dynamicQr"))) {
            diagnostics.add(
                    result(
                            tripId,
                            "DYNAMIC_QR_ONLINE_REQUIRED",
                            "WARNING",
                            entry.id(),
                            "동적 QR은 캡처본으로 사용할 수 없으며 온라인 연결이 필요합니다.",
                            Map.of()));
        }
        if (!Boolean.TRUE.equals(payload.get("offlineReady"))) {
            diagnostics.add(
                    result(
                            tripId,
                            "OFFLINE_NOT_READY",
                            "WARNING",
                            entry.id(),
                            "오프라인에서 확인할 준비가 완료되지 않았습니다.",
                            Map.of()));
        }
    }

    private static boolean empty(Object value) {
        return !(value instanceof Collection<?> collection) || collection.isEmpty();
    }

    private static DiagnosticResult result(
            UUID tripId,
            String code,
            String severity,
            UUID recordId,
            String message,
            Map<String, Object> details) {
        UUID id =
                UUID.nameUUIDFromBytes(
                        (tripId + "|" + code + "|" + recordId).getBytes(StandardCharsets.UTF_8));
        return new DiagnosticResult(id, code, severity, recordId, message, details);
    }
}
