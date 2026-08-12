package com.earthtrip.identity.application.service.support;

import com.earthtrip.identity.application.port.in.SupportRequestUseCase;
import com.earthtrip.identity.application.port.out.PersonalSupportStorePort;
import com.earthtrip.identity.application.port.out.SupportDiagnosticsSerializationPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Duration;
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
class SupportRequestService implements SupportRequestUseCase {

    private static final Duration EXPECTED_RESPONSE_TIME = Duration.ofDays(2);
    private static final String STATUS_LOCATION = "SETTINGS_SUPPORT";

    private static final Set<String> CATEGORIES =
            Set.of("ACCOUNT", "SYNC", "DATA", "PAYMENT", "ROUTE", "RESERVATION", "OTHER");
    private static final Set<String> DIAGNOSTIC_KEYS =
            Set.of(
                    "appVersion",
                    "platform",
                    "osVersion",
                    "deviceModel",
                    "lastRoute",
                    "networkState",
                    "providerStates",
                    "syncCursor",
                    "errorCode");

    private final PersonalSupportStorePort store;
    private final SupportDiagnosticsSerializationPort diagnosticsSerialization;
    private final Clock clock;

    SupportRequestService(
            PersonalSupportStorePort store,
            SupportDiagnosticsSerializationPort diagnosticsSerialization,
            Clock clock) {
        this.store = store;
        this.diagnosticsSerialization = diagnosticsSerialization;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportResult> list(UUID actorUserId) {
        return store.supports(actorUserId).stream().map(SupportRequestService::result).toList();
    }

    @Override
    public SupportResult create(
            UUID actorUserId,
            UUID requestId,
            String category,
            String description,
            String traceId,
            Map<String, Object> diagnostics,
            boolean diagnosticsConsent) {
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "요청 ID가 필요합니다.");
        }
        PersonalSupportStorePort.SupportRecord existing = store.support(requestId).orElse(null);
        if (existing != null) {
            if (!java.util.Objects.equals(existing.userId(), actorUserId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 지원 요청에 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        String safeCategory = category(category);
        String safeDescription = description(description);
        String safeTraceId = traceId(traceId);
        String diagnosticsJson = diagnostics(diagnostics, diagnosticsConsent);
        return result(
                store.saveSupport(
                        new PersonalSupportStorePort.SupportRecord(
                                requestId,
                                actorUserId,
                                safeCategory,
                                safeDescription,
                                safeTraceId,
                                diagnosticsJson,
                                "OPEN",
                                clock.instant())));
    }

    private String diagnostics(Map<String, Object> value, boolean consent) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (!consent) {
            throw EarthTripException.badRequest(
                    "DIAGNOSTICS_CONSENT_REQUIRED", "진단정보를 첨부하려면 명시적으로 동의해야 합니다.");
        }
        if (!DIAGNOSTIC_KEYS.containsAll(value.keySet())) {
            throw EarthTripException.badRequest(
                    "UNSUPPORTED_DIAGNOSTIC_FIELD", "허용되지 않은 진단정보 필드가 포함되어 있습니다.");
        }
        try {
            String json = diagnosticsSerialization.serialize(new LinkedHashMap<>(value));
            if (json.length() > 10_000) {
                throw EarthTripException.badRequest(
                        "DIAGNOSTICS_TOO_LARGE", "진단정보는 10000자 이하여야 합니다.");
            }
            return json;
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.badRequest("INVALID_DIAGNOSTICS", "진단정보를 처리할 수 없습니다.");
        }
    }

    private static String category(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) {
            throw EarthTripException.badRequest("INVALID_SUPPORT_CATEGORY", "지원 요청 분류를 확인해 주세요.");
        }
        return normalized;
    }

    private static String description(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 5_000) {
            throw EarthTripException.badRequest(
                    "INVALID_SUPPORT_DESCRIPTION", "지원 요청 내용은 1자 이상 5000자 이하여야 합니다.");
        }
        return value.strip();
    }

    private static String traceId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 100 || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw EarthTripException.badRequest("INVALID_TRACE_ID", "traceId 형식을 확인해 주세요.");
        }
        return normalized;
    }

    private static SupportResult result(PersonalSupportStorePort.SupportRecord record) {
        return new SupportResult(
                record.id(),
                record.category(),
                record.status(),
                record.createdAt(),
                record.createdAt().plus(EXPECTED_RESPONSE_TIME),
                STATUS_LOCATION);
    }
}
