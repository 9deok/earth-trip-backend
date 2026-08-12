package com.earthtrip.trip.application.service.datecandidate;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripChangePublisher;
import com.earthtrip.trip.application.port.in.DateCandidateUseCase;
import com.earthtrip.trip.application.port.out.DateCandidateStorePort;
import com.earthtrip.trip.domain.DateCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DateCandidateService implements DateCandidateUseCase {
    private final TripAccess access;
    private final DateCandidateStorePort store;
    private final TripChangePublisher changes;
    private final Clock clock;

    DateCandidateService(TripAccess a, DateCandidateStorePort s, TripChangePublisher p, Clock c) {
        access = a;
        store = s;
        changes = p;
        clock = c;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateResult> list(UUID trip, UUID actor) {
        access.requireViewer(trip, actor);
        List<DateCandidate> rows = store.findAll(new TripId(trip));
        Map<UUID, List<DateCandidateStorePort.AvailabilityRecord>> availability =
                store.availability(rows.stream().map(DateCandidate::id).toList());
        return rows.stream()
                .map(row -> result(row, availability.getOrDefault(row.id(), List.of())))
                .toList();
    }

    @Override
    public CandidateResult create(UUID trip, UUID actor, CandidateCommand c) {
        access.requireEditor(trip, actor);
        DateCandidate old = store.findById(c.requestId()).orElse(null);
        if (old != null) {
            if (!old.tripId().value().equals(trip))
                throw EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
            return result(old);
        }
        DateCandidate saved =
                store.save(
                        DateCandidate.create(
                                c.requestId(),
                                new TripId(trip),
                                c.startDate(),
                                c.endDate(),
                                c.note(),
                                actor,
                                clock.instant()));
        changes.publish(trip, actor, "CREATED", "DATE_CANDIDATE", saved.id());
        return result(saved);
    }

    @Override
    public CandidateResult update(UUID trip, UUID id, UUID actor, CandidateCommand c) {
        access.requireEditor(trip, actor);
        DateCandidate d = load(trip, id);
        version(d, c.baseVersion());
        d.update(c.startDate(), c.endDate(), c.note(), c.status(), clock.instant());
        DateCandidate saved = store.save(d);
        changes.publish(trip, actor, "UPDATED", "DATE_CANDIDATE", id);
        return result(saved);
    }

    @Override
    public void delete(UUID trip, UUID id, UUID actor, long v) {
        access.requireEditor(trip, actor);
        DateCandidate d = load(trip, id);
        version(d, v);
        store.delete(id);
        changes.publish(trip, actor, "DELETED", "DATE_CANDIDATE", id);
    }

    @Override
    public AvailabilityResult putAvailability(
            UUID trip, UUID id, UUID actor, String raw, String note) {
        access.requireViewer(trip, actor);
        load(trip, id);
        String value = raw == null ? "AVAILABLE" : raw.strip().toUpperCase(Locale.ROOT);
        if (!List.of("AVAILABLE", "PREFERRED", "UNAVAILABLE", "UNKNOWN").contains(value))
            throw EarthTripException.badRequest("INVALID_AVAILABILITY", "지원하지 않는 가능 여부입니다.");
        DateCandidateStorePort.AvailabilityRecord a =
                store.saveAvailability(
                        id, actor, value, note == null ? null : note.strip(), clock.instant());
        changes.publish(trip, actor, "VOTED", "DATE_CANDIDATE", id);
        return new AvailabilityResult(a.userId(), a.availability(), a.note(), a.updatedAt());
    }

    private DateCandidate load(UUID trip, UUID id) {
        return store.findById(id)
                .filter(d -> d.tripId().value().equals(trip))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "DATE_CANDIDATE_NOT_FOUND", "날짜 후보를 찾을 수 없습니다."));
    }

    private CandidateResult result(DateCandidate d) {
        return new CandidateResult(
                d.id(),
                d.tripId().value(),
                d.startDate(),
                d.endDate(),
                d.note(),
                d.status().name(),
                store.availability(d.id()).stream()
                        .map(
                                a ->
                                        new AvailabilityResult(
                                                a.userId(),
                                                a.availability(),
                                                a.note(),
                                                a.updatedAt()))
                        .toList(),
                d.version(),
                d.createdBy(),
                d.createdAt(),
                d.updatedAt());
    }

    private CandidateResult result(
            DateCandidate d, List<DateCandidateStorePort.AvailabilityRecord> availability) {
        return new CandidateResult(
                d.id(),
                d.tripId().value(),
                d.startDate(),
                d.endDate(),
                d.note(),
                d.status().name(),
                availability.stream()
                        .map(
                                a ->
                                        new AvailabilityResult(
                                                a.userId(),
                                                a.availability(),
                                                a.note(),
                                                a.updatedAt()))
                        .toList(),
                d.version(),
                d.createdBy(),
                d.createdAt(),
                d.updatedAt());
    }

    private static void version(DateCandidate d, long v) {
        if (d.version() != v)
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 날짜 후보 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", d.version()));
    }
}
