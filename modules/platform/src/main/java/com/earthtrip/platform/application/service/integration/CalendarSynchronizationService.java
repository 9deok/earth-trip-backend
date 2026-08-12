package com.earthtrip.platform.application.service.integration;

import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class CalendarSynchronizationService {

    private final IntegrationStorePort store;
    private final TripAccess access;
    private final TripPlanningView planning;
    private final IntegrationProviderRegistry providers;
    private final Clock clock;

    CalendarSynchronizationService(
            IntegrationStorePort store,
            TripAccess access,
            TripPlanningView planning,
            IntegrationProviderRegistry providers,
            Clock clock) {
        this.store = store;
        this.access = access;
        this.planning = planning;
        this.providers = providers;
        this.clock = clock;
    }

    IntegrationUseCase.CalendarSyncResult get(UUID tripId, UUID actor) {
        access.requireViewer(tripId, actor);
        return result(loadCalendar(tripId));
    }

    IntegrationUseCase.CalendarSyncResult put(
            UUID tripId, UUID actor, IntegrationUseCase.CalendarCommand command) {
        access.requireEditor(tripId, actor);
        if (command == null || command.connectionId() == null) {
            throw bad("CALENDAR_CONNECTION_REQUIRED", "캘린더 연결이 필요합니다.");
        }
        IntegrationStorePort.ConnectionRecord connection =
                loadConnection(actor, command.connectionId());
        if (!providers.configured(connection.provider())) {
            throw EarthTripException.unavailable(
                    "CALENDAR_PROVIDER_NOT_CONFIGURED", "캘린더 제공자가 설정되지 않았습니다.");
        }
        IntegrationStorePort.CalendarRecord current = store.calendar(tripId).orElse(null);
        verifyVersion(current == null ? 0 : current.version(), command.baseVersion());
        Instant now = clock.instant();
        return result(
                store.saveCalendar(
                        new IntegrationStorePort.CalendarRecord(
                                tripId,
                                connection.id(),
                                IntegrationPolicy.publicMetadata(command.scopeConfig()),
                                connection.status().equals("ACTIVE")
                                        ? "ACTIVE"
                                        : "REAUTHORIZATION_REQUIRED",
                                actor,
                                current == null ? now : current.createdAt(),
                                now,
                                current == null ? 0 : current.version())));
    }

    void delete(UUID tripId, UUID actor, long baseVersion, boolean deleteExternalCalendar) {
        access.requireEditor(tripId, actor);
        IntegrationStorePort.CalendarRecord calendar = loadCalendar(tripId);
        verifyVersion(calendar.version(), baseVersion);
        if (deleteExternalCalendar) {
            IntegrationStorePort.ConnectionRecord connection =
                    loadConnection(actor, calendar.connectionId());
            providers
                    .require(connection.provider())
                    .deleteCalendar(
                            new ExternalAccountProviderPort.CalendarDeleteCommand(
                                    connection.metadata(), calendar.scopeConfig()));
        }
        store.deleteCalendar(tripId);
    }

    IntegrationUseCase.SyncJobResult run(UUID tripId, UUID actor, UUID requestId) {
        access.requireEditor(tripId, actor);
        if (requestId == null) {
            throw bad("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        IntegrationStorePort.SyncRecord old = store.sync(requestId).orElse(null);
        if (old != null) {
            if (!old.userId().equals(actor) || !"CALENDAR_SYNC".equals(old.jobType())) {
                throw IntegrationPolicy.idempotencyConflict();
            }
            return IntegrationPolicy.syncResult(old);
        }

        IntegrationStorePort.CalendarRecord calendar = loadCalendar(tripId);
        IntegrationStorePort.ConnectionRecord connection =
                loadConnection(actor, calendar.connectionId());
        Instant startedAt = clock.instant();
        IntegrationStorePort.SyncRecord running =
                store.saveSync(
                        new IntegrationStorePort.SyncRecord(
                                requestId,
                                actor,
                                connection.id(),
                                tripId,
                                "CALENDAR_SYNC",
                                connection.status().equals("ACTIVE")
                                        ? "RUNNING"
                                        : "REAUTHORIZATION_REQUIRED",
                                calendar.scopeConfig(),
                                Map.of(),
                                connection.status().equals("ACTIVE")
                                        ? null
                                        : "PROVIDER_REAUTHORIZATION_REQUIRED",
                                1,
                                startedAt,
                                startedAt,
                                0));
        if (!connection.status().equals("ACTIVE")) {
            return IntegrationPolicy.syncResult(running);
        }

        try {
            ExternalAccountProviderPort provider = providers.require(connection.provider());
            TripAccess.PublicTripResult trip = access.publicInfo(tripId);
            ExternalAccountProviderPort.CalendarSyncResult synced =
                    provider.syncCalendar(
                            new ExternalAccountProviderPort.CalendarSyncCommand(
                                    tripId,
                                    trip.title(),
                                    trip.timeZone(),
                                    connection.metadata(),
                                    calendar.scopeConfig(),
                                    calendarEvents(tripId, actor, trip.timeZone())));
            Instant finishedAt = clock.instant();
            store.saveCalendar(copyCalendar(calendar, synced.scopeConfig(), "ACTIVE", finishedAt));
            store.saveConnection(copyConnection(connection, "ACTIVE", null, finishedAt));
            return IntegrationPolicy.syncResult(
                    store.saveSync(
                            copySync(
                                    running,
                                    "SUCCEEDED",
                                    Map.of(
                                            "created", synced.created(),
                                            "updated", synced.updated(),
                                            "deleted", synced.deleted(),
                                            "calendarId",
                                                    string(synced.scopeConfig().get("calendarId"))),
                                    null,
                                    finishedAt)));
        } catch (EarthTripException exception) {
            return failed(running, connection, calendar, exception);
        } catch (RuntimeException exception) {
            Instant failedAt = clock.instant();
            return IntegrationPolicy.syncResult(
                    store.saveSync(
                            copySync(
                                    running,
                                    "FAILED",
                                    Map.of(),
                                    "CALENDAR_SYNC_FAILED",
                                    failedAt)));
        }
    }

    IntegrationUseCase.SyncJobResult runResult(UUID tripId, UUID runId, UUID actor) {
        access.requireViewer(tripId, actor);
        IntegrationStorePort.SyncRecord job =
                store.sync(runId)
                        .filter(candidate -> tripId.equals(candidate.tripId()))
                        .filter(candidate -> candidate.userId().equals(actor))
                        .filter(candidate -> candidate.jobType().equals("CALENDAR_SYNC"))
                        .orElseThrow(IntegrationPolicy::syncNotFound);
        return IntegrationPolicy.syncResult(job);
    }

    private IntegrationUseCase.SyncJobResult failed(
            IntegrationStorePort.SyncRecord running,
            IntegrationStorePort.ConnectionRecord connection,
            IntegrationStorePort.CalendarRecord calendar,
            EarthTripException exception) {
        boolean reauthorization = exception.code().contains("REAUTHORIZATION_REQUIRED");
        Instant failedAt = clock.instant();
        if (reauthorization) {
            store.saveConnection(
                    copyConnection(
                            connection, "REAUTHORIZATION_REQUIRED", exception.code(), failedAt));
            store.saveCalendar(
                    copyCalendar(
                            calendar,
                            calendar.scopeConfig(),
                            "REAUTHORIZATION_REQUIRED",
                            failedAt));
        }
        return IntegrationPolicy.syncResult(
                store.saveSync(
                        copySync(
                                running,
                                reauthorization ? "REAUTHORIZATION_REQUIRED" : "FAILED",
                                Map.of(),
                                exception.code(),
                                failedAt)));
    }

    private List<ExternalAccountProviderPort.CalendarEvent> calendarEvents(
            UUID tripId, UUID actor, String defaultTimeZone) {
        List<ExternalAccountProviderPort.CalendarEvent> events = new ArrayList<>();
        for (TripPlanningView.SearchEntry entry : planning.searchEntries(tripId, actor)) {
            if (!"SCHEDULE_ITEM".equals(entry.type()) || entry.localDate() == null) {
                continue;
            }
            if (Set.of("DELETED", "ARCHIVED", "CANCELLED").contains(entry.status())) {
                continue;
            }
            Map<String, Object> payload = entry.payload();
            String requestedZone = first(payload, "timeZone", "timezone");
            ZoneId zone = validZone(requestedZone, defaultTimeZone);
            String start =
                    dateTime(
                            payload,
                            entry.localDate(),
                            zone,
                            List.of("startAt", "startDateTime", "scheduledAt"),
                            List.of("startTime", "time"));
            String end =
                    dateTime(
                            payload,
                            entry.localDate(),
                            zone,
                            List.of("endAt", "endDateTime"),
                            List.of("endTime"));
            if (!start.isBlank() && end.isBlank()) {
                end = OffsetDateTime.parse(start).plusHours(1).toString();
            }
            events.add(
                    new ExternalAccountProviderPort.CalendarEvent(
                            entry.id(),
                            entry.localDate(),
                            fallback(
                                    first(payload, "title", "name", "placeName", "label"), "여행 일정"),
                            first(payload, "description", "notes", "memo"),
                            first(payload, "address", "placeName", "location"),
                            start,
                            end,
                            zone.getId()));
        }
        return List.copyOf(events);
    }

    private IntegrationStorePort.ConnectionRecord loadConnection(UUID userId, UUID connectionId) {
        return store.connection(connectionId)
                .filter(connection -> connection.userId().equals(userId))
                .filter(connection -> connection.kind().equals("GENERAL"))
                .filter(connection -> connection.revokedAt() == null)
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "INTEGRATION_CONNECTION_NOT_FOUND", "외부 연결을 찾을 수 없습니다."));
    }

    private IntegrationStorePort.CalendarRecord loadCalendar(UUID tripId) {
        return store.calendar(tripId)
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "CALENDAR_SYNC_NOT_FOUND", "캘린더 동기화 설정을 찾을 수 없습니다."));
    }

    private static IntegrationUseCase.CalendarSyncResult result(
            IntegrationStorePort.CalendarRecord calendar) {
        return new IntegrationUseCase.CalendarSyncResult(
                calendar.tripId(),
                calendar.connectionId(),
                calendar.scopeConfig(),
                calendar.status(),
                calendar.updatedAt(),
                calendar.version());
    }

    private static IntegrationStorePort.CalendarRecord copyCalendar(
            IntegrationStorePort.CalendarRecord calendar,
            Map<String, Object> scopeConfig,
            String status,
            Instant now) {
        return new IntegrationStorePort.CalendarRecord(
                calendar.tripId(),
                calendar.connectionId(),
                scopeConfig,
                status,
                calendar.createdBy(),
                calendar.createdAt(),
                now,
                calendar.version());
    }

    private static IntegrationStorePort.ConnectionRecord copyConnection(
            IntegrationStorePort.ConnectionRecord connection,
            String status,
            String errorCode,
            Instant now) {
        return new IntegrationStorePort.ConnectionRecord(
                connection.id(),
                connection.userId(),
                connection.kind(),
                connection.provider(),
                status,
                connection.scopes(),
                connection.metadata(),
                null,
                null,
                status.equals("ACTIVE") ? now : connection.lastSuccessAt(),
                errorCode,
                connection.createdAt(),
                now,
                connection.revokedAt(),
                connection.version());
    }

    private static IntegrationStorePort.SyncRecord copySync(
            IntegrationStorePort.SyncRecord job,
            String status,
            Map<String, Object> result,
            String errorCode,
            Instant now) {
        return new IntegrationStorePort.SyncRecord(
                job.id(),
                job.userId(),
                job.connectionId(),
                job.tripId(),
                job.jobType(),
                status,
                job.request(),
                result,
                errorCode,
                job.attemptCount(),
                job.createdAt(),
                now,
                job.version());
    }

    private static String dateTime(
            Map<String, Object> payload,
            LocalDate date,
            ZoneId zone,
            List<String> dateTimeKeys,
            List<String> timeKeys) {
        for (String key : dateTimeKeys) {
            String value = string(payload.get(key));
            if (!value.isBlank()) {
                try {
                    return OffsetDateTime.parse(value).toString();
                } catch (DateTimeParseException ignored) {
                    try {
                        return LocalDateTime.parse(value)
                                .atZone(zone)
                                .toOffsetDateTime()
                                .toString();
                    } catch (DateTimeParseException invalid) {
                        throw invalidTime();
                    }
                }
            }
        }
        for (String key : timeKeys) {
            String value = string(payload.get(key));
            if (!value.isBlank()) {
                try {
                    LocalTime time = LocalTime.parse(value);
                    return date.atTime(time).atZone(zone).toOffsetDateTime().toString();
                } catch (DateTimeParseException exception) {
                    throw invalidTime();
                }
            }
        }
        return "";
    }

    private static ZoneId validZone(String requested, String fallback) {
        try {
            return ZoneId.of(fallback(requested, fallback));
        } catch (RuntimeException exception) {
            throw bad("INVALID_CALENDAR_TIME_ZONE", "일정 시간대가 올바르지 않습니다.");
        }
    }

    private static String first(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            String value = string(values.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static void verifyVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 연동 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }

    private static EarthTripException invalidTime() {
        return bad("INVALID_CALENDAR_EVENT_TIME", "일정 시각은 HH:mm 또는 ISO-8601 형식이어야 합니다.");
    }

    private static EarthTripException bad(String code, String message) {
        return EarthTripException.badRequest(code, message);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString().strip();
    }
}
