package com.earthtrip.platform.application.service.analysis;

import com.earthtrip.expense.api.ExpenseReceiptAnalysisTarget;
import com.earthtrip.planning.api.PlanningAnalysisTarget;
import com.earthtrip.platform.application.port.in.AnalysisJobUseCase;
import com.earthtrip.platform.application.port.out.AnalysisJobStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AnalysisJobService implements AnalysisJobUseCase {

    private final TripAccess access;
    private final PlanningAnalysisTarget planningTargets;
    private final ExpenseReceiptAnalysisTarget expenseTargets;
    private final AnalysisJobStorePort store;
    private final Clock clock;

    AnalysisJobService(
        TripAccess access,
        PlanningAnalysisTarget planningTargets,
        ExpenseReceiptAnalysisTarget expenseTargets,
        AnalysisJobStorePort store,
        Clock clock
    ) {
        this.access = access;
        this.planningTargets = planningTargets;
        this.expenseTargets = expenseTargets;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public JobResult createResearchSourceJob(
        UUID tripId,
        UUID sourceId,
        UUID actorUserId,
        CreateCommand command
    ) {
        access.requireEditor(tripId, actorUserId);
        planningTargets.getResearchSource(tripId, sourceId, actorUserId);
        return create(tripId, "RESEARCH_SOURCE", sourceId, actorUserId, command);
    }

    @Override
    public JobResult createReceiptJob(
        UUID tripId,
        UUID expenseId,
        UUID actorUserId,
        CreateCommand command
    ) {
        access.requireEditor(tripId, actorUserId);
        expenseTargets.get(tripId, expenseId, actorUserId);
        return create(tripId, "EXPENSE_RECEIPT", expenseId, actorUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResult get(UUID jobId, UUID actorUserId) {
        return result(loadOwned(jobId, actorUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResult> suggestions(UUID jobId, UUID actorUserId) {
        return loadOwned(jobId, actorUserId).suggestions().stream()
            .map(AnalysisJobService::suggestionResult)
            .toList();
    }

    @Override
    public ConfirmationResult confirm(
        UUID jobId,
        UUID actorUserId,
        ConfirmationCommand command
    ) {
        AnalysisJobStorePort.JobRecord job = loadOwned(jobId, actorUserId);
        access.requireEditor(job.tripId(), actorUserId);
        if (command == null || command.requestId() == null || command.confirmedFields() == null) {
            throw EarthTripException.badRequest(
                "INVALID_ANALYSIS_CONFIRMATION", "확정 요청 ID와 교정된 필드가 필요합니다."
            );
        }
        if (job.status().equals("COMPLETED")) {
            if (!command.requestId().equals(job.confirmationRequestId())) {
                throw EarthTripException.conflict(
                    "ANALYSIS_ALREADY_CONFIRMED", "이미 다른 요청으로 확정된 분석 작업입니다."
                );
            }
            return confirmation(job, currentTargetVersion(job, actorUserId));
        }
        requireVersion(job.version(), command.jobBaseVersion());
        if (!job.status().equals("READY")) {
            throw EarthTripException.conflict(
                "ANALYSIS_NOT_CONFIRMABLE", "제안을 확인할 수 있는 분석 작업만 확정할 수 있습니다."
            );
        }
        long targetVersion = switch (job.targetType()) {
            case "RESEARCH_SOURCE" -> planningTargets.confirmResearchSource(
                job.tripId(), job.targetId(), actorUserId, command.confirmedFields(),
                command.targetBaseVersion()
            ).version();
            case "EXPENSE_RECEIPT" -> expenseTargets.confirm(
                job.tripId(), job.targetId(), actorUserId, command.confirmedFields(),
                command.targetBaseVersion()
            ).version();
            default -> throw new IllegalStateException("지원하지 않는 분석 대상입니다.");
        };
        AnalysisJobStorePort.JobRecord completed = store.save(new AnalysisJobStorePort.JobRecord(
            job.id(), job.tripId(), job.targetType(), job.targetId(), job.inputPayload(),
            job.suggestions(), "COMPLETED", command.requestId(),
            safeMap(command.confirmedFields()), null, null, job.attemptCount(), job.createdBy(),
            job.createdAt(), clock.instant(), job.version()
        ));
        return confirmation(completed, targetVersion);
    }

    @Override
    public JobResult retry(UUID jobId, UUID actorUserId, long baseVersion) {
        AnalysisJobStorePort.JobRecord job = loadOwned(jobId, actorUserId);
        access.requireEditor(job.tripId(), actorUserId);
        requireVersion(job.version(), baseVersion);
        if (!job.status().equals("FAILED")) {
            throw EarthTripException.conflict(
                "ANALYSIS_NOT_RETRYABLE", "실패한 분석 작업만 재시도할 수 있습니다."
            );
        }
        return result(store.save(copy(
            job,
            "FAILED",
            "ANALYSIS_RETRY_REQUIRES_SUGGESTIONS",
            "기기 분석 결과를 다시 만든 뒤 새 분석 작업을 시작해 주세요.",
            job.attemptCount() + 1
        )));
    }

    @Override
    public JobResult cancel(UUID jobId, UUID actorUserId, long baseVersion) {
        AnalysisJobStorePort.JobRecord job = loadOwned(jobId, actorUserId);
        access.requireEditor(job.tripId(), actorUserId);
        requireVersion(job.version(), baseVersion);
        if (job.status().equals("CANCELLED")) {
            return result(job);
        }
        if (job.status().equals("COMPLETED")) {
            throw EarthTripException.conflict(
                "ANALYSIS_NOT_CANCELLABLE", "확정된 분석 작업은 취소할 수 없습니다."
            );
        }
        return result(store.save(copy(
            job, "CANCELLED", job.failureCode(), job.failureMessage(), job.attemptCount()
        )));
    }

    private JobResult create(
        UUID tripId,
        String targetType,
        UUID targetId,
        UUID actorUserId,
        CreateCommand command
    ) {
        if (command == null || command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        AnalysisJobStorePort.JobRecord existing = store.find(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId) || !existing.targetType().equals(targetType)
                || !existing.targetId().equals(targetId)
                || !existing.createdBy().equals(actorUserId)) {
                throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "이미 다른 분석 작업에 사용된 요청 ID입니다."
                );
            }
            return result(existing);
        }
        List<Map<String, Object>> suggestions = suggestions(command.suggestions());
        Instant now = clock.instant();
        return result(store.save(new AnalysisJobStorePort.JobRecord(
            command.requestId(), tripId, targetType, targetId,
            safeMap(command.inputPayload()), suggestions,
            suggestions.isEmpty() ? "FAILED" : "READY", null, null,
            suggestions.isEmpty() ? "ANALYSIS_SUGGESTIONS_REQUIRED" : null,
            suggestions.isEmpty()
                ? "서버 분석 공급자가 설정되지 않았습니다. 기기 분석 결과를 함께 보내 주세요."
                : null,
            1, actorUserId, now, now, 0
        )));
    }

    private AnalysisJobStorePort.JobRecord loadOwned(UUID jobId, UUID actorUserId) {
        AnalysisJobStorePort.JobRecord job = store.find(jobId)
            .orElseThrow(() -> EarthTripException.notFound(
                "ANALYSIS_JOB_NOT_FOUND", "분석 작업을 찾을 수 없습니다."
            ));
        access.requireViewer(job.tripId(), actorUserId);
        if (!job.createdBy().equals(actorUserId)) {
            throw EarthTripException.notFound(
                "ANALYSIS_JOB_NOT_FOUND", "분석 작업을 찾을 수 없습니다."
            );
        }
        return job;
    }

    private long currentTargetVersion(
        AnalysisJobStorePort.JobRecord job,
        UUID actorUserId
    ) {
        return switch (job.targetType()) {
            case "RESEARCH_SOURCE" -> planningTargets.getResearchSource(
                job.tripId(), job.targetId(), actorUserId
            ).version();
            case "EXPENSE_RECEIPT" -> expenseTargets.get(
                job.tripId(), job.targetId(), actorUserId
            ).version();
            default -> throw new IllegalStateException("지원하지 않는 분석 대상입니다.");
        };
    }

    private AnalysisJobStorePort.JobRecord copy(
        AnalysisJobStorePort.JobRecord job,
        String status,
        String failureCode,
        String failureMessage,
        int attempts
    ) {
        return new AnalysisJobStorePort.JobRecord(
            job.id(), job.tripId(), job.targetType(), job.targetId(), job.inputPayload(),
            job.suggestions(), status, job.confirmationRequestId(), job.confirmedPayload(),
            failureCode, failureMessage, attempts, job.createdBy(), job.createdAt(),
            clock.instant(), job.version()
        );
    }

    private static List<Map<String, Object>> suggestions(List<SuggestionCommand> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > 200) {
            throw EarthTripException.badRequest(
                "TOO_MANY_ANALYSIS_SUGGESTIONS", "분석 제안은 최대 200개까지 저장할 수 있습니다."
            );
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (SuggestionCommand value : values) {
            if (value == null || value.field() == null || value.field().isBlank()) {
                throw EarthTripException.badRequest(
                    "INVALID_ANALYSIS_SUGGESTION", "분석 제안의 필드 이름이 필요합니다."
                );
            }
            if (value.confidence() != null
                && (value.confidence() < 0 || value.confidence() > 1)) {
                throw EarthTripException.badRequest(
                    "INVALID_ANALYSIS_CONFIDENCE", "분석 신뢰도는 0에서 1 사이여야 합니다."
                );
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", value.field().strip());
            item.put("value", value.value());
            if (value.confidence() != null) item.put("confidence", value.confidence());
            if (value.sourceReference() != null) item.put("sourceReference", value.sourceReference());
            item.put("warnings", value.warnings() == null ? List.of() : List.copyOf(value.warnings()));
            result.add(Collections.unmodifiableMap(item));
        }
        return List.copyOf(result);
    }

    private static SuggestionResult suggestionResult(Map<String, Object> value) {
        Double confidence = value.get("confidence") instanceof Number number
            ? number.doubleValue() : null;
        List<String> warnings = value.get("warnings") instanceof List<?> raw
            ? raw.stream().map(String::valueOf).toList() : List.of();
        return new SuggestionResult(
            String.valueOf(value.get("field")), value.get("value"), confidence,
            value.get("sourceReference") == null
                ? null : String.valueOf(value.get("sourceReference")), warnings
        );
    }

    private static Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    private static ConfirmationResult confirmation(
        AnalysisJobStorePort.JobRecord job,
        long targetVersion
    ) {
        return new ConfirmationResult(
            job.id(), job.targetType(), job.targetId(), job.confirmedPayload(),
            targetVersion, job.status()
        );
    }

    private static JobResult result(AnalysisJobStorePort.JobRecord job) {
        return new JobResult(
            job.id(), job.tripId(), job.targetType(), job.targetId(), job.status(),
            job.suggestions().size(), job.failureCode(), job.failureMessage(),
            job.attemptCount(), job.createdBy(), job.createdAt(), job.updatedAt(), job.version()
        );
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 분석 작업 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", serverVersion)
            );
        }
    }
}
