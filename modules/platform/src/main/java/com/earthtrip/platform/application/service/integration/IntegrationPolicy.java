package com.earthtrip.platform.application.service.integration;

import com.earthtrip.platform.application.port.in.IntegrationUseCase.SyncJobResult;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class IntegrationPolicy {
    private IntegrationPolicy() {}

    static SyncJobResult syncResult(IntegrationStorePort.SyncRecord job) {
        return new SyncJobResult(
                job.id(),
                job.connectionId(),
                job.tripId(),
                job.jobType(),
                job.status(),
                job.result(),
                job.errorCode(),
                job.attemptCount(),
                job.createdAt(),
                job.updatedAt(),
                job.version());
    }

    static Map<String, Object> publicMetadata(Map<String, Object> values) {
        if (values == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    if (key != null && !key.startsWith("_") && value != null) {
                        result.put(key, value);
                    }
                });
        return immutable(result);
    }

    static Set<String> scopes(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        return Set.copyOf(result);
    }

    static String kind(String kind) {
        String value = strip(kind).toUpperCase(Locale.ROOT);
        if (!Set.of("GENERAL", "FINANCIAL").contains(value)) {
            throw bad("INVALID_CONNECTION_KIND", "지원하지 않는 외부 연결 종류입니다.");
        }
        return value;
    }

    static String provider(String provider) {
        String value = strip(provider).toUpperCase(Locale.ROOT);
        if (value.isBlank() || value.length() > 40) {
            throw bad("INVALID_INTEGRATION_PROVIDER", "외부 연결 제공자를 확인해 주세요.");
        }
        return value;
    }

    static void requireSameScope(
            IntegrationStorePort.ConnectionRecord connection, UUID userId, String kind) {
        if (!connection.userId().equals(userId) || !connection.kind().equals(kind)) {
            throw idempotencyConflict();
        }
    }

    static void verifyVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 연동 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }

    static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 다른 연동 요청에 사용된 ID입니다.");
    }

    static EarthTripException syncNotFound() {
        return EarthTripException.notFound("INTEGRATION_SYNC_JOB_NOT_FOUND", "연동 작업을 찾을 수 없습니다.");
    }

    static Map<String, Object> immutable(Map<String, Object> values) {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    static EarthTripException bad(String code, String message) {
        return EarthTripException.badRequest(code, message);
    }

    static String strip(String value) {
        return value == null ? "" : value.strip();
    }
}
