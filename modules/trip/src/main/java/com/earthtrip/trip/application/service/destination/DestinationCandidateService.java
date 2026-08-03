package com.earthtrip.trip.application.service.destination;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.application.port.in.DestinationCandidateUseCase;
import com.earthtrip.trip.application.port.out.DestinationCandidateStorePort;
import com.earthtrip.trip.domain.DestinationCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
class DestinationCandidateService implements DestinationCandidateUseCase {
    private final TripAccess access; private final DestinationCandidateStorePort store; private final Clock clock;
    DestinationCandidateService(TripAccess access, DestinationCandidateStorePort store, Clock clock) {
        this.access = access; this.store = store; this.clock = clock;
    }
    @Override @Transactional(readOnly = true)
    public List<CandidateResult> list(UUID tripId, UUID actor) {
        access.requireViewer(tripId, actor);
        return store.findAll(new TripId(tripId)).stream().map(this::result).toList();
    }
    @Override public CandidateResult create(UUID tripId, UUID actor, CandidateCommand c) {
        access.requireEditor(tripId, actor);
        DestinationCandidate existing = store.findById(c.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().value().equals(tripId)) throw idConflict();
            return result(existing);
        }
        DestinationCandidate candidate = DestinationCandidate.create(
            c.requestId(), new TripId(tripId), c.name(), c.countryCode(), c.placeId(),
            c.latitude(), c.longitude(), c.note(), actor, clock.instant()
        );
        return result(store.save(candidate));
    }
    @Override public CandidateResult update(UUID tripId, UUID id, UUID actor, CandidateCommand c) {
        access.requireEditor(tripId, actor); DestinationCandidate candidate = load(tripId, id);
        verifyVersion(candidate, c.baseVersion());
        candidate.update(c.name(), c.countryCode(), c.placeId(), c.latitude(), c.longitude(),
            c.note(), c.status(), clock.instant());
        return result(store.save(candidate));
    }
    @Override public void delete(UUID tripId, UUID id, UUID actor, long baseVersion) {
        access.requireEditor(tripId, actor); DestinationCandidate candidate = load(tripId, id);
        verifyVersion(candidate, baseVersion); store.delete(id);
    }
    @Override public PreferenceResult putPreference(UUID tripId, UUID id, UUID actor, String raw) {
        access.requireViewer(tripId, actor); load(tripId, id);
        String preference = raw == null ? "INTERESTED" : raw.strip().toUpperCase(Locale.ROOT);
        if (!List.of("INTERESTED", "PREFERRED", "NOT_INTERESTED").contains(preference)) {
            throw EarthTripException.badRequest("INVALID_PREFERENCE", "지원하지 않는 선호 값입니다.");
        }
        return preference(store.savePreference(id, actor, preference, clock.instant()));
    }
    @Override public void deletePreference(UUID tripId, UUID id, UUID actor) {
        access.requireViewer(tripId, actor); load(tripId, id); store.deletePreference(id, actor);
    }
    private DestinationCandidate load(UUID tripId, UUID id) {
        return store.findById(id).filter(c -> c.tripId().value().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound("DESTINATION_CANDIDATE_NOT_FOUND", "여행지 후보를 찾을 수 없습니다."));
    }
    private CandidateResult result(DestinationCandidate c) {
        return new CandidateResult(
            c.id(), c.tripId().value(), c.name(), c.countryCode(), c.placeId(), c.latitude(),
            c.longitude(), c.note(), c.status().name(),
            store.preferences(c.id()).stream().map(DestinationCandidateService::preference).toList(),
            c.version(), c.createdBy(), c.createdAt(), c.updatedAt()
        );
    }
    private static PreferenceResult preference(DestinationCandidateStorePort.PreferenceRecord p) {
        return new PreferenceResult(p.userId(), p.preference(), p.updatedAt());
    }
    private static void verifyVersion(DestinationCandidate c, long baseVersion) {
        if (c.version() != baseVersion) throw new EarthTripException(
            "VERSION_CONFLICT", 409, "다른 후보 변경이 먼저 저장되었습니다.",
            java.util.Map.of("serverVersion", c.version())
        );
    }
    private static EarthTripException idConflict() {
        return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 사용된 요청 ID입니다.");
    }
}
