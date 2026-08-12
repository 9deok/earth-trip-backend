package com.earthtrip.expense.application.service.statement;

import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.expense.application.port.in.StatementImportUseCase;
import com.earthtrip.expense.application.port.out.StatementImportStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
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
class StatementImportService implements StatementImportUseCase {

    private final TripAccess access;
    private final ExpenseUseCase expenses;
    private final StatementImportStorePort store;
    private final Clock clock;

    StatementImportService(
            TripAccess access,
            ExpenseUseCase expenses,
            StatementImportStorePort store,
            Clock clock) {
        this.access = access;
        this.expenses = expenses;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return store.findImports(tripId).stream().map(this::result).toList();
    }

    @Override
    public ImportResult create(UUID tripId, UUID actorUserId, ImportCommand command) {
        access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        StatementImportStorePort.ImportRecord existing =
                store.findImport(command.requestId()).orElse(null);
        if (existing != null) {
            requireTrip(existing.tripId(), tripId);
            return result(existing);
        }
        List<CandidateCommand> candidates = requireCandidates(command.candidates());
        if (candidates.stream().map(CandidateCommand::candidateId).distinct().count()
                != candidates.size()) {
            throw EarthTripException.badRequest(
                    "DUPLICATE_STATEMENT_CANDIDATE_ID", "중복된 카드 내역 후보 ID가 있습니다.");
        }
        Instant now = clock.instant();
        StatementImportStorePort.ImportRecord saved =
                store.saveImport(
                        new StatementImportStorePort.ImportRecord(
                                command.requestId(),
                                tripId,
                                source(command.source()),
                                "READY",
                                actorUserId,
                                now,
                                now,
                                0));
        for (CandidateCommand candidate : candidates) {
            validateCandidate(
                    candidate.title(),
                    candidate.amountMinor(),
                    candidate.currency(),
                    candidate.occurredAt(),
                    candidate.payerUserId(),
                    candidate.payload());
            StatementImportStorePort.CandidateRecord duplicate =
                    store.findCandidate(candidate.candidateId()).orElse(null);
            if (duplicate != null) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 카드 내역 후보에 사용된 ID입니다.");
            }
            store.saveCandidate(
                    new StatementImportStorePort.CandidateRecord(
                            candidate.candidateId(),
                            saved.id(),
                            tripId,
                            candidate.title().strip(),
                            candidate.amountMinor(),
                            currency(candidate.currency()),
                            candidate.occurredAt(),
                            candidate.payerUserId(),
                            payload(candidate.payload()),
                            "READY",
                            null,
                            now,
                            now,
                            0));
        }
        return result(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateResult> candidates(UUID tripId, UUID importId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        loadImport(tripId, importId);
        return store.findCandidates(importId).stream().map(StatementImportService::result).toList();
    }

    @Override
    public CandidateResult updateCandidate(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            CandidateUpdate command) {
        access.requireEditor(tripId, actorUserId);
        StatementImportStorePort.CandidateRecord current =
                loadCandidate(tripId, importId, candidateId);
        mutable(current);
        version(current.version(), command.baseVersion());
        String title = command.title() == null ? current.title() : command.title().strip();
        long amount = command.amountMinor() == null ? current.amountMinor() : command.amountMinor();
        String currency =
                command.currency() == null ? current.currency() : currency(command.currency());
        Instant occurredAt =
                command.occurredAt() == null ? current.occurredAt() : command.occurredAt();
        UUID payer = command.payerUserId() == null ? current.payerUserId() : command.payerUserId();
        Map<String, Object> payload =
                command.payload() == null ? current.payload() : payload(command.payload());
        validateCandidate(title, amount, currency, occurredAt, payer, payload);
        return result(
                store.saveCandidate(
                        copy(
                                current,
                                title,
                                amount,
                                currency,
                                occurredAt,
                                payer,
                                payload,
                                current.status(),
                                current.expenseId())));
    }

    @Override
    public CandidateResult linkExpense(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            UUID expenseId,
            long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        ExpenseUseCase.ExpenseResult expense = expenses.get(tripId, expenseId, actorUserId);
        StatementImportStorePort.CandidateRecord current =
                loadCandidate(tripId, importId, candidateId);
        mutable(current);
        version(current.version(), baseVersion);
        if (!expense.currency().equals(current.currency())
                || expense.amountMinor() != current.amountMinor()) {
            throw EarthTripException.badRequest(
                    "STATEMENT_EXPENSE_MISMATCH", "통화와 금액이 같은 지출에만 카드 내역 후보를 연결할 수 있습니다.");
        }
        return result(
                store.saveCandidate(
                        copy(
                                current,
                                current.title(),
                                current.amountMinor(),
                                current.currency(),
                                current.occurredAt(),
                                current.payerUserId(),
                                current.payload(),
                                "LINKED",
                                expenseId)));
    }

    @Override
    public CandidateResult unlinkExpense(
            UUID tripId, UUID importId, UUID candidateId, UUID actorUserId, long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        StatementImportStorePort.CandidateRecord current =
                loadCandidate(tripId, importId, candidateId);
        version(current.version(), baseVersion);
        if (!current.status().equals("LINKED")) {
            return result(current);
        }
        return result(
                store.saveCandidate(
                        copy(
                                current,
                                current.title(),
                                current.amountMinor(),
                                current.currency(),
                                current.occurredAt(),
                                current.payerUserId(),
                                current.payload(),
                                "READY",
                                null)));
    }

    @Override
    public CandidateResult dismiss(
            UUID tripId,
            UUID importId,
            UUID candidateId,
            UUID actorUserId,
            String reason,
            long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        StatementImportStorePort.CandidateRecord current =
                loadCandidate(tripId, importId, candidateId);
        mutable(current);
        version(current.version(), baseVersion);
        String normalized = reason == null || reason.isBlank() ? null : reason.strip();
        if (normalized != null && normalized.length() > 500) {
            throw EarthTripException.badRequest(
                    "STATEMENT_DISMISSAL_REASON_TOO_LONG", "제외 사유는 500자 이하여야 합니다.");
        }
        Map<String, Object> payload = new LinkedHashMap<>(current.payload());
        if (normalized != null) {
            payload.put("dismissalReason", normalized);
        }
        CandidateResult result =
                result(
                        store.saveCandidate(
                                copy(
                                        current,
                                        current.title(),
                                        current.amountMinor(),
                                        current.currency(),
                                        current.occurredAt(),
                                        current.payerUserId(),
                                        Map.copyOf(payload),
                                        "DISMISSED",
                                        null)));
        refreshImportStatus(tripId, importId);
        return result;
    }

    @Override
    public ConfirmationResult confirm(
            UUID tripId, UUID importId, UUID actorUserId, List<ConfirmationItem> items) {
        access.requireEditor(tripId, actorUserId);
        loadImport(tripId, importId);
        if (items == null
                || items.isEmpty()
                || items.stream().map(ConfirmationItem::candidateId).distinct().count()
                        != items.size()) {
            throw EarthTripException.badRequest(
                    "INVALID_STATEMENT_CONFIRMATION", "확정할 카드 내역 후보를 중복 없이 선택해 주세요.");
        }
        List<ExpenseUseCase.ExpenseResult> created = new ArrayList<>();
        for (ConfirmationItem item : items) {
            StatementImportStorePort.CandidateRecord candidate =
                    loadCandidate(tripId, importId, item.candidateId());
            version(candidate.version(), item.baseVersion());
            if (candidate.status().equals("LINKED") || candidate.status().equals("CONFIRMED")) {
                created.add(expenses.get(tripId, candidate.expenseId(), actorUserId));
                continue;
            }
            if (!candidate.status().equals("READY") || item.expenseRequestId() == null) {
                throw EarthTripException.conflict(
                        "STATEMENT_CANDIDATE_NOT_CONFIRMABLE", "확정할 수 없는 카드 내역 후보입니다.");
            }
            ExpenseUseCase.ExpenseResult expense =
                    expenses.create(
                            tripId,
                            actorUserId,
                            new ExpenseUseCase.ExpenseCommand(
                                    item.expenseRequestId(),
                                    candidate.title(),
                                    item.categoryCode(),
                                    candidate.amountMinor(),
                                    candidate.currency(),
                                    candidate.occurredAt(),
                                    Map.of(candidate.payerUserId(), candidate.amountMinor()),
                                    item.participantShares(),
                                    item.visibility(),
                                    "RECORDED",
                                    "카드 내역에서 가져옴",
                                    0));
            store.saveCandidate(
                    copy(
                            candidate,
                            candidate.title(),
                            candidate.amountMinor(),
                            candidate.currency(),
                            candidate.occurredAt(),
                            candidate.payerUserId(),
                            candidate.payload(),
                            "CONFIRMED",
                            expense.expenseId()));
            created.add(expense);
        }
        StatementImportStorePort.ImportRecord updated = refreshImportStatus(tripId, importId);
        return new ConfirmationResult(importId, List.copyOf(created), updated.status());
    }

    private StatementImportStorePort.ImportRecord refreshImportStatus(UUID tripId, UUID importId) {
        StatementImportStorePort.ImportRecord current = loadImport(tripId, importId);
        List<StatementImportStorePort.CandidateRecord> candidates = store.findCandidates(importId);
        boolean complete =
                candidates.stream()
                        .allMatch(
                                candidate ->
                                        Set.of("CONFIRMED", "LINKED", "DISMISSED")
                                                .contains(candidate.status()));
        boolean touched =
                candidates.stream().anyMatch(candidate -> !candidate.status().equals("READY"));
        String status = complete ? "COMPLETED" : touched ? "PARTIAL" : "READY";
        return store.saveImport(
                new StatementImportStorePort.ImportRecord(
                        current.id(),
                        tripId,
                        current.source(),
                        status,
                        current.createdBy(),
                        current.createdAt(),
                        clock.instant(),
                        current.version()));
    }

    private StatementImportStorePort.ImportRecord loadImport(UUID tripId, UUID importId) {
        return store.findImport(importId)
                .filter(item -> item.tripId().equals(tripId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "STATEMENT_IMPORT_NOT_FOUND", "카드 내역 가져오기를 찾을 수 없습니다."));
    }

    private StatementImportStorePort.CandidateRecord loadCandidate(
            UUID tripId, UUID importId, UUID candidateId) {
        loadImport(tripId, importId);
        return store.findCandidate(candidateId)
                .filter(item -> item.tripId().equals(tripId) && item.importId().equals(importId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "STATEMENT_CANDIDATE_NOT_FOUND", "카드 내역 후보를 찾을 수 없습니다."));
    }

    private StatementImportStorePort.CandidateRecord copy(
            StatementImportStorePort.CandidateRecord current,
            String title,
            long amount,
            String currency,
            Instant occurredAt,
            UUID payer,
            Map<String, Object> payload,
            String status,
            UUID expenseId) {
        return new StatementImportStorePort.CandidateRecord(
                current.id(),
                current.importId(),
                current.tripId(),
                title,
                amount,
                currency,
                occurredAt,
                payer,
                payload,
                status,
                expenseId,
                current.createdAt(),
                clock.instant(),
                current.version());
    }

    private ImportResult result(StatementImportStorePort.ImportRecord record) {
        return new ImportResult(
                record.id(),
                record.source(),
                record.status(),
                store.findCandidates(record.id()).size(),
                record.createdBy(),
                record.createdAt(),
                record.updatedAt(),
                record.version());
    }

    private static CandidateResult result(StatementImportStorePort.CandidateRecord record) {
        return new CandidateResult(
                record.id(),
                record.importId(),
                record.title(),
                record.amountMinor(),
                record.currency(),
                record.occurredAt(),
                record.payerUserId(),
                record.payload(),
                record.status(),
                record.expenseId(),
                record.createdAt(),
                record.updatedAt(),
                record.version());
    }

    private static List<CandidateCommand> requireCandidates(List<CandidateCommand> values) {
        if (values == null
                || values.isEmpty()
                || values.size() > 500
                || values.stream().anyMatch(item -> item == null || item.candidateId() == null)) {
            throw EarthTripException.badRequest(
                    "INVALID_STATEMENT_CANDIDATES", "1~500개의 카드 내역 후보가 필요합니다.");
        }
        return values;
    }

    private static void validateCandidate(
            String title,
            long amount,
            String currency,
            Instant occurredAt,
            UUID payer,
            Map<String, Object> payload) {
        if (title == null
                || title.isBlank()
                || title.strip().length() > 200
                || amount <= 0
                || occurredAt == null
                || payer == null
                || payload != null && payload.size() > 100) {
            throw EarthTripException.badRequest(
                    "INVALID_STATEMENT_CANDIDATE", "카드 내역 후보 정보를 확인해 주세요.");
        }
        currency(currency);
    }

    private static Map<String, Object> payload(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    private static String source(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 80) {
            throw EarthTripException.badRequest("INVALID_STATEMENT_SOURCE", "카드 내역 출처를 확인해 주세요.");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private static String currency(String value) {
        try {
            return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (RuntimeException exception) {
            throw EarthTripException.badRequest("INVALID_CURRENCY", "유효한 ISO 4217 통화 코드가 아닙니다.");
        }
    }

    private static void mutable(StatementImportStorePort.CandidateRecord current) {
        if (Set.of("CONFIRMED", "DISMISSED").contains(current.status())) {
            throw EarthTripException.conflict(
                    "STATEMENT_CANDIDATE_FINALIZED", "이미 확정 또는 제외된 후보는 수정할 수 없습니다.");
        }
    }

    private static void version(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 카드 내역 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }

    private static void requireTrip(UUID actual, UUID expected) {
        if (!actual.equals(expected)) {
            throw EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "이미 다른 여행의 가져오기에 사용된 요청 ID입니다.");
        }
    }
}
