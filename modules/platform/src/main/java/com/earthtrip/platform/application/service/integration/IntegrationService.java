package com.earthtrip.platform.application.service.integration;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class IntegrationService implements IntegrationUseCase {

    private final IntegrationStorePort store;
    private final TripAccess access;
    private final IntegrationProviderRegistry providers;
    private final CalendarSynchronizationService calendars;
    private final Clock clock;

    IntegrationService(
        IntegrationStorePort store,
        TripAccess access,
        IntegrationProviderRegistry providers,
        CalendarSynchronizationService calendars,
        Clock clock
    ) {
        this.store = store;
        this.access = access;
        this.providers = providers;
        this.calendars = calendars;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionResult> connections(UUID userId, String kind) {
        return store.connections(userId, kind(kind)).stream().map(this::connectionResult).toList();
    }

    @Override
    public ConnectionResult createConnection(
        UUID userId,
        String kind,
        ConnectionCommand command
    ) {
        String normalizedKind = kind(kind);
        if (command == null || command.requestId() == null) {
            throw bad("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        IntegrationStorePort.ConnectionRecord old = store.connection(command.requestId())
            .orElse(null);
        if (old != null) {
            requireSameScope(old, userId, normalizedKind);
            if ("AUTHORIZATION_REQUIRED".equals(old.status())
                && !strip(command.authorizationCode()).isBlank()) {
                return completeAuthorization(old, command);
            }
            return connectionResult(old);
        }

        String providerName = provider(command.provider());
        ExternalAccountProviderPort externalProvider = providers.require(providerName);
        Instant now = clock.instant();
        Map<String, Object> metadata = publicMetadata(command.metadata());
        Set<String> requestedScopes = scopes(command.scopes());
        if (strip(command.authorizationCode()).isBlank()) {
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (command.requestId() + ":" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)
            );
            return connectionResult(store.saveConnection(
                new IntegrationStorePort.ConnectionRecord(
                    command.requestId(), userId, normalizedKind, providerName,
                    "AUTHORIZATION_REQUIRED", requestedScopes, metadata, state,
                    now.plus(Duration.ofMinutes(10)), null,
                    externalProvider.configured() ? null : "PROVIDER_NOT_CONFIGURED",
                    now, now, null, 0
                )
            ));
        }

        ExternalAccountProviderPort.AuthorizationResult authorization =
            externalProvider.authorize(new ExternalAccountProviderPort.AuthorizationCommand(
                command.authorizationCode(),
                command.redirectUri(),
                command.codeVerifier(),
                requestedScopes
            ));
        Map<String, Object> protectedMetadata = new LinkedHashMap<>(metadata);
        protectedMetadata.putAll(authorization.protectedMetadata());
        return connectionResult(store.saveConnection(new IntegrationStorePort.ConnectionRecord(
            command.requestId(), userId, normalizedKind, providerName, "ACTIVE",
            authorization.grantedScopes(), immutable(protectedMetadata), null, null, now,
            null, now, now, null, 0
        )));
    }

    private ConnectionResult completeAuthorization(
        IntegrationStorePort.ConnectionRecord pending,
        ConnectionCommand command
    ) {
        ExternalAccountProviderPort provider = providers.require(pending.provider());
        ExternalAccountProviderPort.AuthorizationResult authorization = provider.authorize(
            new ExternalAccountProviderPort.AuthorizationCommand(
                command.authorizationCode(),
                command.redirectUri(),
                command.codeVerifier(),
                pending.scopes()
            )
        );
        Map<String, Object> metadata = new LinkedHashMap<>(publicMetadata(pending.metadata()));
        metadata.putAll(publicMetadata(command.metadata()));
        metadata.putAll(authorization.protectedMetadata());
        Instant now = clock.instant();
        return connectionResult(store.saveConnection(new IntegrationStorePort.ConnectionRecord(
            pending.id(),
            pending.userId(),
            pending.kind(),
            pending.provider(),
            "ACTIVE",
            authorization.grantedScopes(),
            immutable(metadata),
            null,
            null,
            now,
            null,
            pending.createdAt(),
            now,
            null,
            pending.version()
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionResult connection(UUID userId, UUID connectionId, String kind) {
        return connectionResult(loadConnection(userId, connectionId, kind(kind)));
    }

    @Override
    public void deleteConnection(
        UUID userId,
        UUID connectionId,
        String kind,
        long baseVersion
    ) {
        IntegrationStorePort.ConnectionRecord connection = loadConnection(
            userId,
            connectionId,
            kind(kind)
        );
        verifyVersion(connection.version(), baseVersion);
        providers.find(connection.provider()).ifPresent(provider -> {
            try {
                provider.revoke(connection.metadata());
            } catch (RuntimeException ignored) {
                // 제공자 철회 실패가 로컬 비밀 삭제를 막지 않게 한다.
            }
        });
        Instant now = clock.instant();
        store.saveConnection(new IntegrationStorePort.ConnectionRecord(
            connection.id(), connection.userId(), connection.kind(), connection.provider(),
            "REVOKED", connection.scopes(), publicMetadata(connection.metadata()), null, null,
            connection.lastSuccessAt(), null, connection.createdAt(), now, now,
            connection.version()
        ));
    }

    @Override
    public SyncJobResult syncConnection(
        UUID userId,
        UUID connectionId,
        UUID requestId,
        Map<String, Object> payload
    ) {
        IntegrationStorePort.ConnectionRecord connection = loadConnection(
            userId,
            connectionId,
            "GENERAL"
        );
        if (!connection.status().equals("ACTIVE")) {
            return createFinishedJob(
                requestId, userId, connectionId, null, "CONNECTION_SYNC", connection, payload,
                "REAUTHORIZATION_REQUIRED", Map.of(), "INTEGRATION_REAUTHORIZATION_REQUIRED"
            );
        }
        ExternalAccountProviderPort provider = providers.require(connection.provider());
        if (!provider.configured()) {
            return createFinishedJob(
                requestId, userId, connectionId, null, "CONNECTION_SYNC", connection, payload,
                "FAILED", Map.of(), "PROVIDER_NOT_CONFIGURED"
            );
        }
        try {
            ExternalAccountProviderPort.ConnectionCheckResult check = provider.checkConnection(
                connection.metadata()
            );
            Instant now = clock.instant();
            store.saveConnection(new IntegrationStorePort.ConnectionRecord(
                connection.id(), connection.userId(), connection.kind(), connection.provider(),
                check.status(), connection.scopes(), connection.metadata(), null, null, now,
                null, connection.createdAt(), now, null, connection.version()
            ));
            return createFinishedJob(
                requestId, userId, connectionId, null, "CONNECTION_SYNC", connection, payload,
                "SUCCEEDED", check.details(), null
            );
        } catch (EarthTripException exception) {
            boolean reauthorizationRequired = exception.httpStatus() == 401
                || exception.httpStatus() == 403
                || exception.code().contains("REAUTHORIZATION_REQUIRED")
                || exception.code().contains("INVALID_GRANT");
            Instant now = clock.instant();
            store.saveConnection(new IntegrationStorePort.ConnectionRecord(
                connection.id(), connection.userId(), connection.kind(), connection.provider(),
                reauthorizationRequired ? "REAUTHORIZATION_REQUIRED" : connection.status(),
                connection.scopes(), connection.metadata(), null, null,
                connection.lastSuccessAt(), exception.code(), connection.createdAt(), now,
                null, connection.version()
            ));
            return createFinishedJob(
                requestId, userId, connectionId, null, "CONNECTION_SYNC", connection, payload,
                reauthorizationRequired ? "REAUTHORIZATION_REQUIRED" : "FAILED",
                Map.of(), exception.code()
            );
        }
    }

    @Override
    public SyncJobResult syncJob(UUID userId, UUID jobId) {
        IntegrationStorePort.SyncRecord job = store.sync(jobId)
            .filter(record -> record.userId().equals(userId))
            .orElseThrow(IntegrationService::syncNotFound);
        if (job.status().equals("QUEUED")) {
            job = finish(
                job,
                "FAILED",
                Map.of(),
                "INTEGRATION_EXECUTOR_NOT_AVAILABLE",
                clock.instant()
            );
        }
        return syncResult(job);
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarSyncResult calendar(UUID tripId, UUID actorUserId) {
        return calendars.get(tripId, actorUserId);
    }

    @Override
    public CalendarSyncResult putCalendar(
        UUID tripId,
        UUID actorUserId,
        CalendarCommand command
    ) {
        return calendars.put(tripId, actorUserId, command);
    }

    @Override
    public void deleteCalendar(UUID tripId, UUID actorUserId, long baseVersion) {
        calendars.delete(tripId, actorUserId, baseVersion);
    }

    @Override
    public SyncJobResult runCalendar(UUID tripId, UUID actorUserId, UUID requestId) {
        return calendars.run(tripId, actorUserId, requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public SyncJobResult calendarRun(UUID tripId, UUID runId, UUID actorUserId) {
        return calendars.runResult(tripId, runId, actorUserId);
    }

    @Override
    public SyncJobResult providerStatementImport(
        UUID tripId,
        UUID actorUserId,
        UUID requestId,
        UUID connectionId,
        Map<String, Object> payload
    ) {
        access.requireEditor(tripId, actorUserId);
        IntegrationStorePort.ConnectionRecord connection = loadConnection(
            actorUserId,
            connectionId,
            "FINANCIAL"
        );
        return createFinishedJob(
            requestId,
            actorUserId,
            connectionId,
            tripId,
            "PROVIDER_STATEMENT_IMPORT",
            connection,
            payload,
            "FAILED",
            Map.of(),
            "FINANCIAL_IMPORT_PROVIDER_NOT_IMPLEMENTED"
        );
    }

    private SyncJobResult createFinishedJob(
        UUID requestId,
        UUID userId,
        UUID connectionId,
        UUID tripId,
        String jobType,
        IntegrationStorePort.ConnectionRecord connection,
        Map<String, Object> payload,
        String status,
        Map<String, Object> result,
        String errorCode
    ) {
        if (requestId == null) {
            throw bad("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        IntegrationStorePort.SyncRecord old = store.sync(requestId).orElse(null);
        if (old != null) {
            if (!old.userId().equals(userId) || !old.jobType().equals(jobType)) {
                throw idempotencyConflict();
            }
            return syncResult(old);
        }
        Instant now = clock.instant();
        return syncResult(store.saveSync(new IntegrationStorePort.SyncRecord(
            requestId, userId, connectionId, tripId, jobType,
            status,
            publicMetadata(payload), publicMetadata(result), errorCode,
            1, now, now, 0
        )));
    }

    private IntegrationStorePort.SyncRecord finish(
        IntegrationStorePort.SyncRecord job,
        String status,
        Map<String, Object> result,
        String errorCode,
        Instant now
    ) {
        return store.saveSync(new IntegrationStorePort.SyncRecord(
            job.id(), job.userId(), job.connectionId(), job.tripId(), job.jobType(),
            status, job.request(), result, errorCode, job.attemptCount(), job.createdAt(),
            now, job.version()
        ));
    }

    private IntegrationStorePort.ConnectionRecord loadConnection(
        UUID userId,
        UUID connectionId,
        String kind
    ) {
        return store.connection(connectionId)
            .filter(connection -> connection.userId().equals(userId))
            .filter(connection -> connection.kind().equals(kind))
            .filter(connection -> connection.revokedAt() == null)
            .orElseThrow(() -> EarthTripException.notFound(
                "INTEGRATION_CONNECTION_NOT_FOUND",
                "외부 연결을 찾을 수 없습니다."
            ));
    }

    private ConnectionResult connectionResult(IntegrationStorePort.ConnectionRecord connection) {
        return new ConnectionResult(
            connection.id(), connection.kind(), connection.provider(), connection.status(),
            connection.scopes(), publicMetadata(connection.metadata()),
            connection.authorizationState(), connection.authorizationExpiresAt(),
            connection.lastSuccessAt(), connection.errorCode(),
            providers.configured(connection.provider()), connection.createdAt(),
            connection.updatedAt(), connection.version()
        );
    }

    static SyncJobResult syncResult(IntegrationStorePort.SyncRecord job) {
        return new SyncJobResult(
            job.id(), job.connectionId(), job.tripId(), job.jobType(), job.status(),
            job.result(), job.errorCode(), job.attemptCount(), job.createdAt(),
            job.updatedAt(), job.version()
        );
    }

    static Map<String, Object> publicMetadata(Map<String, Object> values) {
        if (values == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && !key.startsWith("_") && value != null) {
                result.put(key, value);
            }
        });
        return immutable(result);
    }

    private static Set<String> scopes(Set<String> values) {
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

    private static String kind(String kind) {
        String value = strip(kind).toUpperCase(Locale.ROOT);
        if (!Set.of("GENERAL", "FINANCIAL").contains(value)) {
            throw bad("INVALID_CONNECTION_KIND", "지원하지 않는 외부 연결 종류입니다.");
        }
        return value;
    }

    private static String provider(String provider) {
        String value = strip(provider).toUpperCase(Locale.ROOT);
        if (value.isBlank() || value.length() > 40) {
            throw bad("INVALID_INTEGRATION_PROVIDER", "외부 연결 제공자를 확인해 주세요.");
        }
        return value;
    }

    private static void requireSameScope(
        IntegrationStorePort.ConnectionRecord connection,
        UUID userId,
        String kind
    ) {
        if (!connection.userId().equals(userId) || !connection.kind().equals(kind)) {
            throw idempotencyConflict();
        }
    }

    private static void verifyVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 연동 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", serverVersion)
            );
        }
    }

    static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict(
            "IDEMPOTENCY_KEY_REUSED",
            "이미 다른 연동 요청에 사용된 ID입니다."
        );
    }

    static EarthTripException syncNotFound() {
        return EarthTripException.notFound(
            "INTEGRATION_SYNC_JOB_NOT_FOUND",
            "연동 작업을 찾을 수 없습니다."
        );
    }

    private static Map<String, Object> immutable(Map<String, Object> values) {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static EarthTripException bad(String code, String message) {
        return EarthTripException.badRequest(code, message);
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }
}
