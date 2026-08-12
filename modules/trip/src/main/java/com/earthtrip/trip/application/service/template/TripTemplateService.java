package com.earthtrip.trip.application.service.template;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.CreateTripCommand;
import com.earthtrip.trip.application.port.in.CreateTripUseCase;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.in.TripTemplateUseCase;
import com.earthtrip.trip.application.port.out.TripTemplateStorePort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class TripTemplateService implements TripTemplateUseCase {

    private static final Set<String> ALLOWED_SCOPES = Set.of("BASIC", "STRUCTURE");

    private final TripAccess access;
    private final TripStructureView structures;
    private final CreateTripUseCase createTrips;
    private final TripManagementUseCase trips;
    private final TripSegmentUseCase segments;
    private final TripTemplateStorePort store;
    private final Clock clock;

    TripTemplateService(
            TripAccess access,
            TripStructureView structures,
            CreateTripUseCase createTrips,
            TripManagementUseCase trips,
            TripSegmentUseCase segments,
            TripTemplateStorePort store,
            Clock clock) {
        this.access = access;
        this.structures = structures;
        this.createTrips = createTrips;
        this.trips = trips;
        this.segments = segments;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResult> list(UUID actorUserId) {
        return store.findAll(actorUserId).stream().map(TripTemplateService::result).toList();
    }

    @Override
    public TemplateResult create(UUID actorUserId, CreateCommand command) {
        if (command == null || command.requestId() == null || command.sourceTripId() == null) {
            throw EarthTripException.badRequest("INVALID_TRIP_TEMPLATE", "템플릿 ID와 원본 여행이 필요합니다.");
        }
        access.requireViewer(command.sourceTripId(), actorUserId);
        TripTemplateStorePort.TemplateRecord existing =
                store.find(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.ownerUserId().equals(actorUserId)
                    || !existing.sourceTripId().equals(command.sourceTripId())) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 여행 템플릿에 사용된 요청 ID입니다.");
            }
            return result(existing);
        }
        Set<String> scopes = scopes(command.includeScopes());
        Instant now = clock.instant();
        return result(
                store.save(
                        new TripTemplateStorePort.TemplateRecord(
                                command.requestId(),
                                actorUserId,
                                command.sourceTripId(),
                                name(command.name()),
                                description(command.description()),
                                scopes,
                                snapshot(command.sourceTripId(), actorUserId, scopes),
                                now,
                                now,
                                null,
                                0)));
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResult get(UUID templateId, UUID actorUserId) {
        return result(load(templateId, actorUserId));
    }

    @Override
    public TemplateResult update(UUID templateId, UUID actorUserId, UpdateCommand command) {
        TripTemplateStorePort.TemplateRecord current = load(templateId, actorUserId);
        requireVersion(current.version(), command.baseVersion());
        Set<String> scopes =
                command.includeScopes() == null
                        ? current.includeScopes()
                        : scopes(command.includeScopes());
        Map<String, Object> snapshot =
                scopes.equals(current.includeScopes())
                        ? current.snapshot()
                        : snapshot(current.sourceTripId(), actorUserId, scopes);
        return result(
                store.save(
                        new TripTemplateStorePort.TemplateRecord(
                                current.id(),
                                current.ownerUserId(),
                                current.sourceTripId(),
                                command.name() == null ? current.name() : name(command.name()),
                                command.description() == null
                                        ? current.description()
                                        : description(command.description()),
                                scopes,
                                snapshot,
                                current.createdAt(),
                                clock.instant(),
                                null,
                                current.version())));
    }

    @Override
    public void delete(UUID templateId, UUID actorUserId, long baseVersion) {
        TripTemplateStorePort.TemplateRecord current = load(templateId, actorUserId);
        requireVersion(current.version(), baseVersion);
        store.save(
                new TripTemplateStorePort.TemplateRecord(
                        current.id(),
                        current.ownerUserId(),
                        current.sourceTripId(),
                        current.name(),
                        current.description(),
                        current.includeScopes(),
                        current.snapshot(),
                        current.createdAt(),
                        clock.instant(),
                        clock.instant(),
                        current.version()));
    }

    @Override
    public TripManagementUseCase.TripResult createDraft(
            UUID templateId, UUID actorUserId, DraftCommand command) {
        TripTemplateStorePort.TemplateRecord template = load(templateId, actorUserId);
        if (command == null || command.requestId() == null) {
            throw EarthTripException.badRequest(
                    "DRAFT_REQUEST_ID_REQUIRED", "초안 여행의 requestId가 필요합니다.");
        }
        TripTemplateStorePort.DraftRecord existing =
                store.findDraft(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.templateId().equals(templateId)
                    || !existing.createdBy().equals(actorUserId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 템플릿 초안에 사용된 요청 ID입니다.");
            }
            return trips.get(existing.tripId(), actorUserId);
        }
        Map<String, Object> tripSnapshot = objectMap(template.snapshot().get("trip"));
        String timeZone = valueOr(command.timeZone(), text(tripSnapshot, "timeZone"));
        String currency = valueOr(command.defaultCurrency(), text(tripSnapshot, "defaultCurrency"));
        String title =
                command.title() == null || command.title().isBlank()
                        ? template.name()
                        : command.title().strip();
        createTrips.create(
                new CreateTripCommand(command.requestId(), actorUserId, title, timeZone, currency));
        TripManagementUseCase.TripResult created = trips.get(command.requestId(), actorUserId);
        LocalDate sourceStart = date(tripSnapshot.get("startDate"));
        LocalDate sourceEnd = date(tripSnapshot.get("endDate"));
        LocalDate draftStart = command.startDate() == null ? sourceStart : command.startDate();
        LocalDate draftEnd = shiftedEnd(sourceStart, sourceEnd, draftStart);
        TripManagementUseCase.TripResult draft =
                trips.update(
                        created.tripId(),
                        actorUserId,
                        new TripManagementUseCase.UpdateTripCommand(
                                title,
                                "DRAFT",
                                draftStart,
                                draftEnd,
                                timeZone,
                                currency,
                                text(tripSnapshot, "planningMode"),
                                text(tripSnapshot, "pace"),
                                created.version()));
        if (template.includeScopes().contains("STRUCTURE")) {
            createSegments(template, draft, actorUserId, sourceStart, draftStart);
        }
        store.saveDraft(
                new TripTemplateStorePort.DraftRecord(
                        command.requestId(),
                        templateId,
                        draft.tripId(),
                        actorUserId,
                        clock.instant()));
        return trips.get(draft.tripId(), actorUserId);
    }

    private void createSegments(
            TripTemplateStorePort.TemplateRecord template,
            TripManagementUseCase.TripResult draft,
            UUID actorUserId,
            LocalDate sourceStart,
            LocalDate draftStart) {
        List<?> snapshotSegments = list(template.snapshot().get("segments"));
        long shiftDays =
                sourceStart == null || draftStart == null
                        ? 0
                        : ChronoUnit.DAYS.between(sourceStart, draftStart);
        for (Object raw : snapshotSegments) {
            Map<String, Object> segment = objectMap(raw);
            UUID sourceId = UUID.fromString(text(segment, "segmentId"));
            UUID segmentId =
                    UUID.nameUUIDFromBytes(
                            ("earthtrip:trip-template:"
                                            + template.id()
                                            + ":"
                                            + draft.tripId()
                                            + ":"
                                            + sourceId)
                                    .getBytes(StandardCharsets.UTF_8));
            segments.create(
                    draft.tripId(),
                    actorUserId,
                    new TripSegmentUseCase.SegmentCommand(
                            segmentId,
                            text(segment, "type"),
                            nullableText(segment, "cityName"),
                            nullableText(segment, "countryCode"),
                            nullableText(segment, "placeId"),
                            decimal(segment.get("latitude")),
                            decimal(segment.get("longitude")),
                            shift(date(segment.get("startDate")), shiftDays),
                            shift(date(segment.get("endDate")), shiftDays),
                            nullableText(segment, "accommodationName"),
                            nullableText(segment, "accommodationPlaceId"),
                            shift(instant(segment.get("checkInAt")), shiftDays),
                            shift(instant(segment.get("checkOutAt")), shiftDays),
                            nullableText(segment, "transportMode"),
                            shift(instant(segment.get("departureAt")), shiftDays),
                            shift(instant(segment.get("arrivalAt")), shiftDays),
                            number(segment, "sortOrder").intValue(),
                            0));
        }
    }

    private Map<String, Object> snapshot(UUID sourceTripId, UUID actorUserId, Set<String> scopes) {
        TripStructureView.StructureSnapshot source = structures.snapshot(sourceTripId, actorUserId);
        Map<String, Object> trip = new LinkedHashMap<>();
        put(trip, "startDate", source.trip().startDate());
        put(trip, "endDate", source.trip().endDate());
        put(trip, "timeZone", source.trip().timeZone());
        put(trip, "defaultCurrency", source.trip().defaultCurrency());
        put(trip, "planningMode", source.trip().planningMode());
        put(trip, "pace", source.trip().pace());
        List<Map<String, Object>> segmentSnapshots =
                scopes.contains("STRUCTURE")
                        ? source.segments().stream()
                                .map(TripTemplateService::segmentSnapshot)
                                .toList()
                        : List.of();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("trip", trip);
        snapshot.put("segments", segmentSnapshots);
        return snapshot;
    }

    private static Map<String, Object> segmentSnapshot(TripStructureView.Segment segment) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "segmentId", segment.segmentId());
        put(result, "type", segment.type());
        put(result, "cityName", segment.cityName());
        put(result, "countryCode", segment.countryCode());
        put(result, "placeId", segment.placeId());
        put(result, "latitude", segment.latitude());
        put(result, "longitude", segment.longitude());
        put(result, "startDate", segment.startDate());
        put(result, "endDate", segment.endDate());
        put(result, "accommodationName", segment.accommodationName());
        put(result, "accommodationPlaceId", segment.accommodationPlaceId());
        put(result, "checkInAt", segment.checkInAt());
        put(result, "checkOutAt", segment.checkOutAt());
        put(result, "transportMode", segment.transportMode());
        put(result, "departureAt", segment.departureAt());
        put(result, "arrivalAt", segment.arrivalAt());
        put(result, "sortOrder", segment.sortOrder());
        return result;
    }

    private TripTemplateStorePort.TemplateRecord load(UUID templateId, UUID actorUserId) {
        return store.find(templateId)
                .filter(
                        template ->
                                template.deletedAt() == null
                                        && template.ownerUserId().equals(actorUserId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "TRIP_TEMPLATE_NOT_FOUND", "여행 템플릿을 찾을 수 없습니다."));
    }

    private static Set<String> scopes(Set<String> raw) {
        Set<String> result = new LinkedHashSet<>();
        result.add("BASIC");
        if (raw == null || raw.isEmpty()) {
            result.add("STRUCTURE");
        } else {
            raw.stream().map(value -> value.strip().toUpperCase(Locale.ROOT)).forEach(result::add);
        }
        if (!ALLOWED_SCOPES.containsAll(result)) {
            throw EarthTripException.badRequest(
                    "INVALID_TRIP_TEMPLATE_SCOPE", "지원하지 않는 여행 템플릿 포함 범위입니다.");
        }
        return Set.copyOf(result);
    }

    private static String name(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw EarthTripException.badRequest(
                    "INVALID_TRIP_TEMPLATE_NAME", "템플릿 이름은 1~120자여야 합니다.");
        }
        return value.strip();
    }

    private static String description(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 500) {
            throw EarthTripException.badRequest(
                    "TRIP_TEMPLATE_DESCRIPTION_TOO_LONG", "템플릿 설명은 500자 이하여야 합니다.");
        }
        return normalized;
    }

    private static TemplateResult result(TripTemplateStorePort.TemplateRecord template) {
        return new TemplateResult(
                template.id(),
                template.sourceTripId(),
                template.name(),
                template.description(),
                template.includeScopes(),
                list(template.snapshot().get("segments")).size(),
                template.createdAt(),
                template.updatedAt(),
                template.version());
    }

    private static LocalDate shiftedEnd(
            LocalDate sourceStart, LocalDate sourceEnd, LocalDate draftStart) {
        if (draftStart != null && (sourceStart == null || sourceEnd == null)) {
            return draftStart;
        }
        if (sourceStart == null || sourceEnd == null || draftStart == null) {
            return sourceEnd;
        }
        return draftStart.plusDays(ChronoUnit.DAYS.between(sourceStart, sourceEnd));
    }

    private static LocalDate shift(LocalDate value, long days) {
        return value == null ? null : value.plusDays(days);
    }

    private static Instant shift(Instant value, long days) {
        return value == null ? null : value.plus(Duration.ofDays(days));
    }

    private static LocalDate date(Object value) {
        return value == null ? null : LocalDate.parse(String.valueOf(value));
    }

    private static Instant instant(Object value) {
        return value == null ? null : Instant.parse(String.valueOf(value));
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private static Number number(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        return raw instanceof Number number ? number : new BigDecimal(String.valueOf(raw));
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalStateException("여행 템플릿 스냅샷 형식이 올바르지 않습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> result ? result : List.of();
    }

    private static String text(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalStateException("여행 템플릿 필수 값이 없습니다: " + key);
        }
        return String.valueOf(raw);
    }

    private static String nullableText(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    private static String valueOr(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.strip();
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value.toString());
        }
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 템플릿 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }
}
