package com.earthtrip.wallet.application.service.reservationimport;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.wallet.application.port.in.ReservationImportUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ReservationImportService implements ReservationImportUseCase {

    private static final Set<String> SOURCE_TYPES = Set.of(
        "FILE", "SCREENSHOT", "EMAIL", "FORWARDED_EMAIL", "MANUAL"
    );
    private static final Set<String> CANDIDATE_TYPES = Set.of(
        "FLIGHT", "LODGING", "TRANSPORT", "ACTIVITY", "RESTAURANT", "OTHER"
    );
    private static final Set<String> TERMINAL_JOB_STATUSES = Set.of(
        "COMPLETED", "CANCELLED"
    );

    private final TripAccess access;
    private final WalletRecordUseCase records;
    private final ReservationImportStorePort store;
    private final Clock clock;

    ReservationImportService(
        TripAccess access,
        WalletRecordUseCase records,
        ReservationImportStorePort store,
        Clock clock
    ) {
        this.access = access;
        this.records = records;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public ImportResult create(UUID tripId, UUID actorUserId, ImportCommand command) {
        access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        ReservationImportStorePort.JobRecord existing = store.findJob(command.requestId())
            .orElse(null);
        if (existing != null) {
            requireTrip(existing.tripId(), tripId);
            return result(existing);
        }
        String sourceType = normalizedSourceType(command.sourceType());
        Map<String, Object> sourcePayload = safePayload(command.sourcePayload());
        List<CandidateCommand> candidates = command.candidates() == null
            ? List.of()
            : List.copyOf(command.candidates());
        validateCandidateIds(candidates);
        Instant now = clock.instant();
        ReservationImportStorePort.JobRecord saved = store.saveJob(
            new ReservationImportStorePort.JobRecord(
                command.requestId(), tripId, sourceType, sourcePayload,
                candidates.isEmpty() ? "QUEUED" : "READY", null, null, 1,
                actorUserId, now, now, 0
            )
        );
        for (CandidateCommand candidate : candidates) {
            store.saveCandidate(new ReservationImportStorePort.CandidateRecord(
                candidate.candidateId(), saved.id(), tripId, title(candidate.title()),
                candidateType(candidate.candidateType()), safePayload(candidate.payload()),
                confidence(candidate.confidence()), "READY", null, null, now, now, 0
            ));
        }
        return result(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResult get(UUID jobId, UUID actorUserId) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireViewer(job.tripId(), actorUserId);
        return result(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateResult> candidates(UUID jobId, UUID actorUserId) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireViewer(job.tripId(), actorUserId);
        return store.findCandidates(jobId).stream()
            .map(ReservationImportService::result)
            .toList();
    }

    @Override
    public ConfirmationResult confirm(
        UUID jobId,
        UUID actorUserId,
        List<ConfirmationItem> items
    ) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireEditor(job.tripId(), actorUserId);
        requireActive(job);
        if (items == null || items.isEmpty()) {
            throw EarthTripException.badRequest(
                "RESERVATION_IMPORT_CONFIRMATION_REQUIRED", "확정할 예약 후보가 필요합니다."
            );
        }
        if (items.stream().map(ConfirmationItem::candidateId).anyMatch(java.util.Objects::isNull)
            || items.stream().map(ConfirmationItem::candidateId).distinct().count()
                != items.size()) {
            throw EarthTripException.badRequest(
                "DUPLICATE_RESERVATION_IMPORT_CANDIDATE",
                "확정할 예약 후보를 중복 없이 선택해 주세요."
            );
        }
        List<WalletRecordUseCase.RecordResult> reservations = new ArrayList<>();
        for (ConfirmationItem item : items) {
            ReservationImportStorePort.CandidateRecord candidate = loadCandidate(job, item.candidateId());
            requireVersion(candidate.version(), item.baseVersion());
            if (candidate.status().equals("CONFIRMED")) {
                reservations.add(records.get(
                    job.tripId(), actorUserId, "RESERVATION", candidate.reservationId()
                ));
                continue;
            }
            if (!candidate.status().equals("READY") || item.reservationRequestId() == null) {
                throw EarthTripException.conflict(
                    "RESERVATION_IMPORT_CANDIDATE_NOT_CONFIRMABLE",
                    "확정할 수 없는 예약 후보입니다."
                );
            }
            Map<String, Object> payload = reservationPayload(job, candidate, item);
            WalletRecordUseCase.RecordResult reservation = records.create(
                job.tripId(), actorUserId, "RESERVATION", false,
                new WalletRecordUseCase.Command(
                    item.reservationRequestId(), null, payload, "ACTIVE",
                    item.visibility(), item.sortOrder(), 0
                )
            );
            store.saveCandidate(copy(
                candidate, "CONFIRMED", reservation.id(), null, clock.instant()
            ));
            reservations.add(reservation);
        }
        ReservationImportStorePort.JobRecord updated = refreshJob(job);
        return new ConfirmationResult(jobId, List.copyOf(reservations), updated.status());
    }

    @Override
    public ImportResult dismiss(
        UUID jobId,
        UUID actorUserId,
        List<DismissalItem> items
    ) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireEditor(job.tripId(), actorUserId);
        requireActive(job);
        if (items == null || items.isEmpty()
            || items.stream().map(DismissalItem::candidateId).anyMatch(java.util.Objects::isNull)
            || items.stream().map(DismissalItem::candidateId).distinct().count()
                != items.size()) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_IMPORT_DISMISSAL",
                "제외할 예약 후보를 중복 없이 선택해 주세요."
            );
        }
        for (DismissalItem item : items) {
            ReservationImportStorePort.CandidateRecord candidate = loadCandidate(job, item.candidateId());
            requireVersion(candidate.version(), item.baseVersion());
            if (candidate.status().equals("DISMISSED")) {
                continue;
            }
            if (!candidate.status().equals("READY")) {
                throw EarthTripException.conflict(
                    "RESERVATION_IMPORT_CANDIDATE_NOT_DISMISSIBLE",
                    "확정된 예약 후보는 제외할 수 없습니다."
                );
            }
            store.saveCandidate(copy(
                candidate, "DISMISSED", null, reason(item.reason()), clock.instant()
            ));
        }
        return result(refreshJob(job));
    }

    @Override
    public ImportResult retry(UUID jobId, UUID actorUserId, long baseVersion) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireEditor(job.tripId(), actorUserId);
        requireVersion(job.version(), baseVersion);
        if (!job.status().equals("FAILED")) {
            throw EarthTripException.conflict(
                "RESERVATION_IMPORT_NOT_RETRYABLE", "실패한 예약 가져오기만 재시도할 수 있습니다."
            );
        }
        return result(store.saveJob(copyJob(
            job, "QUEUED", null, null, job.attemptCount() + 1
        )));
    }

    @Override
    public ImportResult cancel(UUID jobId, UUID actorUserId, long baseVersion) {
        ReservationImportStorePort.JobRecord job = loadJob(jobId);
        access.requireEditor(job.tripId(), actorUserId);
        requireVersion(job.version(), baseVersion);
        if (job.status().equals("CANCELLED")) {
            return result(job);
        }
        if (job.status().equals("COMPLETED")) {
            throw EarthTripException.conflict(
                "RESERVATION_IMPORT_NOT_CANCELLABLE",
                "완료된 예약 가져오기는 취소할 수 없습니다."
            );
        }
        return result(store.saveJob(copyJob(
            job, "CANCELLED", job.failureCode(), job.failureMessage(), job.attemptCount()
        )));
    }

    private Map<String, Object> reservationPayload(
        ReservationImportStorePort.JobRecord job,
        ReservationImportStorePort.CandidateRecord candidate,
        ConfirmationItem item
    ) {
        Map<String, Object> payload = new LinkedHashMap<>(candidate.payload());
        if (item.payloadOverride() != null) {
            payload.putAll(item.payloadOverride());
        }
        payload.putIfAbsent("title", candidate.title());
        payload.putIfAbsent("reservationType", candidate.candidateType());
        payload.put("importJobId", job.id().toString());
        payload.put("importCandidateId", candidate.id().toString());
        return immutablePayload(payload);
    }

    private ReservationImportStorePort.JobRecord refreshJob(
        ReservationImportStorePort.JobRecord job
    ) {
        List<ReservationImportStorePort.CandidateRecord> candidates = store.findCandidates(job.id());
        boolean complete = !candidates.isEmpty() && candidates.stream().allMatch(candidate ->
            Set.of("CONFIRMED", "DISMISSED").contains(candidate.status())
        );
        boolean touched = candidates.stream().anyMatch(candidate -> !candidate.status().equals("READY"));
        String status = complete ? "COMPLETED" : touched ? "PARTIAL" : "READY";
        return store.saveJob(copyJob(
            job, status, job.failureCode(), job.failureMessage(), job.attemptCount()
        ));
    }

    private ReservationImportStorePort.JobRecord copyJob(
        ReservationImportStorePort.JobRecord job,
        String status,
        String failureCode,
        String failureMessage,
        int attempts
    ) {
        return new ReservationImportStorePort.JobRecord(
            job.id(), job.tripId(), job.sourceType(), job.sourcePayload(), status,
            failureCode, failureMessage, attempts, job.createdBy(), job.createdAt(),
            clock.instant(), job.version()
        );
    }

    private static ReservationImportStorePort.CandidateRecord copy(
        ReservationImportStorePort.CandidateRecord candidate,
        String status,
        UUID reservationId,
        String dismissalReason,
        Instant updatedAt
    ) {
        return new ReservationImportStorePort.CandidateRecord(
            candidate.id(), candidate.jobId(), candidate.tripId(), candidate.title(),
            candidate.candidateType(), candidate.payload(), candidate.confidence(), status,
            reservationId, dismissalReason, candidate.createdAt(), updatedAt, candidate.version()
        );
    }

    private ReservationImportStorePort.JobRecord loadJob(UUID jobId) {
        return store.findJob(jobId).orElseThrow(() -> EarthTripException.notFound(
            "RESERVATION_IMPORT_NOT_FOUND", "예약 가져오기 작업을 찾을 수 없습니다."
        ));
    }

    private ReservationImportStorePort.CandidateRecord loadCandidate(
        ReservationImportStorePort.JobRecord job,
        UUID candidateId
    ) {
        return store.findCandidate(candidateId)
            .filter(candidate -> candidate.jobId().equals(job.id())
                && candidate.tripId().equals(job.tripId()))
            .orElseThrow(() -> EarthTripException.notFound(
                "RESERVATION_IMPORT_CANDIDATE_NOT_FOUND", "예약 가져오기 후보를 찾을 수 없습니다."
            ));
    }

    private static void validateCandidateIds(List<CandidateCommand> candidates) {
        Set<UUID> ids = new HashSet<>();
        for (CandidateCommand candidate : candidates) {
            if (candidate == null || candidate.candidateId() == null || !ids.add(candidate.candidateId())) {
                throw EarthTripException.badRequest(
                    "DUPLICATE_RESERVATION_IMPORT_CANDIDATE",
                    "후보 ID는 비어 있지 않고 서로 달라야 합니다."
                );
            }
        }
    }

    private static void requireActive(ReservationImportStorePort.JobRecord job) {
        if (TERMINAL_JOB_STATUSES.contains(job.status())) {
            throw EarthTripException.conflict(
                "RESERVATION_IMPORT_FINISHED", "종료된 예약 가져오기는 변경할 수 없습니다."
            );
        }
    }

    private static String normalizedSourceType(String sourceType) {
        String normalized = sourceType == null ? "" : sourceType.strip().toUpperCase();
        if (!SOURCE_TYPES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_IMPORT_SOURCE", "지원하지 않는 예약 가져오기 원본입니다."
            );
        }
        return normalized;
    }

    private static String candidateType(String type) {
        String normalized = type == null ? "OTHER" : type.strip().toUpperCase();
        if (!CANDIDATE_TYPES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_CANDIDATE_TYPE", "지원하지 않는 예약 후보 유형입니다."
            );
        }
        return normalized;
    }

    private static BigDecimal confidence(BigDecimal value) {
        if (value != null
            && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_IMPORT_CONFIDENCE", "후보 신뢰도는 0에서 1 사이여야 합니다."
            );
        }
        return value;
    }

    private static String title(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 200) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_IMPORT_TITLE", "예약 후보 제목은 1~200자여야 합니다."
            );
        }
        return value.strip();
    }

    private static String reason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 500) {
            throw EarthTripException.badRequest(
                "RESERVATION_IMPORT_REASON_TOO_LONG", "제외 사유는 500자 이하여야 합니다."
            );
        }
        return normalized;
    }

    private static Map<String, Object> safePayload(Map<String, Object> value) {
        return immutablePayload(value == null ? Map.of() : value);
    }

    private static Map<String, Object> immutablePayload(Map<String, Object> value) {
        if (value.keySet().stream().anyMatch(java.util.Objects::isNull)) {
            throw EarthTripException.badRequest(
                "INVALID_RESERVATION_IMPORT_PAYLOAD", "예약 가져오기 데이터의 키는 비어 있을 수 없습니다."
            );
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    private static void requireTrip(UUID actual, UUID expected) {
        if (!actual.equals(expected)) {
            throw EarthTripException.conflict(
                "IDEMPOTENCY_KEY_REUSED", "이미 다른 여행의 가져오기에 사용된 요청 ID입니다."
            );
        }
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 예약 가져오기 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", serverVersion)
            );
        }
    }

    private ImportResult result(ReservationImportStorePort.JobRecord job) {
        return new ImportResult(
            job.id(), job.tripId(), job.sourceType(), job.sourcePayload(), job.status(),
            job.failureCode(), job.failureMessage(), job.attemptCount(),
            store.findCandidates(job.id()).size(), job.createdBy(), job.createdAt(),
            job.updatedAt(), job.version()
        );
    }

    private static CandidateResult result(ReservationImportStorePort.CandidateRecord candidate) {
        return new CandidateResult(
            candidate.id(), candidate.jobId(), candidate.title(), candidate.candidateType(),
            candidate.payload(), candidate.confidence(), candidate.status(),
            candidate.reservationId(), candidate.dismissalReason(), candidate.createdAt(),
            candidate.updatedAt(), candidate.version()
        );
    }
}
