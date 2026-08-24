package com.earthtrip.expense.application.service.expense;

import com.earthtrip.expense.application.port.in.ExpenseCategoryUseCase;
import com.earthtrip.expense.application.port.in.ExpenseUseCase;
import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.domain.Expense;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.spi.TripChangePublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ExpenseService implements ExpenseUseCase {

    private final TripAccess access;
    private final ExpenseStorePort store;
    private final ExpenseCategoryUseCase categories;
    private final TripChangePublisher changes;
    private final Clock clock;

    ExpenseService(
            TripAccess access,
            ExpenseStorePort store,
            ExpenseCategoryUseCase categories,
            TripChangePublisher changes,
            Clock clock) {
        this.access = access;
        this.store = store;
        this.categories = categories;
        this.changes = changes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return store.findAll(tripId).stream()
                .filter(expense -> visible(expense, actorUserId))
                .map(ExpenseService::result)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResult get(UUID tripId, UUID expenseId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        Expense expense = load(tripId, expenseId);
        if (!visible(expense, actorUserId)) {
            throw notFound();
        }
        return result(expense);
    }

    @Override
    public ExpenseResult create(UUID tripId, UUID actorUserId, ExpenseCommand command) {
        access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        Expense existing = store.findById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId) || !existing.createdBy().equals(actorUserId)) {
                throw idempotencyConflict();
            }
            return result(existing);
        }
        String category = categories.requireCategoryCode(tripId, command.categoryCode());
        Expense expense =
                Expense.create(
                        command.requestId(),
                        tripId,
                        command.title(),
                        category,
                        required(command.amountMinor()),
                        command.currency(),
                        command.occurredAt(),
                        command.payerContributions(),
                        command.participantShares(),
                        command.visibility() == null ? "TRIP" : command.visibility(),
                        command.note(),
                        actorUserId,
                        clock.instant());
        Expense saved = store.save(expense);
        changes.publish(tripId, actorUserId, "CREATED", "EXPENSE", saved.id());
        return result(saved);
    }

    @Override
    public ExpenseResult update(
            UUID tripId, UUID expenseId, UUID actorUserId, ExpenseCommand command) {
        access.requireEditor(tripId, actorUserId);
        Expense expense = load(tripId, expenseId);
        requireVisible(expense, actorUserId);
        requireVersion(expense, command.baseVersion());
        String category =
                command.categoryCode() == null
                        ? null
                        : categories.requireCategoryCode(tripId, command.categoryCode());
        expense.update(
                command.title(),
                category,
                command.amountMinor(),
                command.currency(),
                command.occurredAt(),
                command.payerContributions(),
                command.participantShares(),
                command.visibility(),
                command.status(),
                command.note(),
                actorUserId,
                clock.instant());
        Expense saved = store.save(expense);
        changes.publish(tripId, actorUserId, "UPDATED", "EXPENSE", saved.id());
        return result(saved);
    }

    @Override
    public void delete(UUID tripId, UUID expenseId, UUID actorUserId, long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        Expense expense = load(tripId, expenseId);
        requireVisible(expense, actorUserId);
        requireVersion(expense, baseVersion);
        expense.delete(actorUserId, clock.instant());
        store.save(expense);
        changes.publish(tripId, actorUserId, "DELETED", "EXPENSE", expenseId);
    }

    @Override
    public List<ExpenseResult> split(
            UUID tripId,
            UUID expenseId,
            UUID actorUserId,
            long baseVersion,
            List<SplitPart> parts) {
        access.requireEditor(tripId, actorUserId);
        Expense original = load(tripId, expenseId);
        requireVisible(original, actorUserId);
        requireVersion(original, baseVersion);
        if (!original.status().equals("RECORDED")) {
            throw EarthTripException.conflict("EXPENSE_NOT_SPLITTABLE", "기록 상태의 지출만 분할할 수 있습니다.");
        }
        validateSplit(original, parts);
        Instant now = clock.instant();
        List<ExpenseResult> results = new ArrayList<>();
        for (SplitPart part : parts) {
            if (part.requestId() == null || store.findById(part.requestId()).isPresent()) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "분할 요청 ID가 이미 사용되었거나 비어 있습니다.");
            }
            String category = categories.requireCategoryCode(tripId, part.categoryCode());
            Expense child =
                    Expense.create(
                            part.requestId(),
                            tripId,
                            part.title(),
                            category,
                            part.amountMinor(),
                            original.currency(),
                            original.occurredAt(),
                            part.payerContributions(),
                            part.participantShares(),
                            original.visibility(),
                            original.note(),
                            actorUserId,
                            now);
            results.add(result(store.save(child)));
        }
        original.markSplit(actorUserId, now);
        store.save(original);
        changes.publish(tripId, actorUserId, "SPLIT", "EXPENSE", expenseId);
        return List.copyOf(results);
    }

    @Override
    public AdjustmentResult refund(
            UUID tripId,
            UUID expenseId,
            UUID actorUserId,
            UUID requestId,
            long amountMinor,
            UUID participantId,
            Map<String, Object> payload) {
        access.requireEditor(tripId, actorUserId);
        Expense expense = load(tripId, expenseId);
        requireVisible(expense, actorUserId);
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        ExpenseStorePort.AdjustmentRecord existing = store.findAdjustment(requestId).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                    || !existing.expenseId().equals(expenseId)
                    || !existing.kind().equals("REFUND")
                    || existing.amountMinor() != amountMinor
                    || !java.util.Objects.equals(existing.participantId(), participantId)) {
                throw idempotencyConflict();
            }
            return adjustment(existing);
        }
        long refunded;
        try {
            refunded =
                    store.findAdjustments(expenseId).stream()
                            .filter(adjustment -> adjustment.kind().equals("REFUND"))
                            .mapToLong(ExpenseStorePort.AdjustmentRecord::amountMinor)
                            .reduce(0, Math::addExact);
        } catch (ArithmeticException exception) {
            throw EarthTripException.badRequest(
                    "REFUND_TOTAL_OVERFLOW", "누적 환불 금액이 지원 범위를 벗어났습니다.");
        }
        if (amountMinor <= 0 || amountMinor > expense.amountMinor() - refunded) {
            throw EarthTripException.badRequest("INVALID_REFUND_AMOUNT", "환불 금액을 확인해 주세요.");
        }
        Instant now = clock.instant();
        ExpenseStorePort.AdjustmentRecord saved =
                store.saveAdjustment(
                        new ExpenseStorePort.AdjustmentRecord(
                                requestId,
                                tripId,
                                expenseId,
                                "REFUND",
                                amountMinor,
                                expense.currency(),
                                participantId,
                                payload == null ? Map.of() : Map.copyOf(payload),
                                actorUserId,
                                now));
        changes.publish(
                tripId,
                actorUserId,
                "REFUNDED",
                "EXPENSE",
                expenseId,
                Map.of("adjustmentId", saved.id()));
        return adjustment(saved);
    }

    private Expense load(UUID tripId, UUID expenseId) {
        return store.findById(expenseId)
                .filter(expense -> expense.tripId().equals(tripId))
                .orElseThrow(ExpenseService::notFound);
    }

    private static void validateSplit(Expense original, List<SplitPart> parts) {
        if (parts == null || parts.size() < 2) {
            throw EarthTripException.badRequest("INVALID_SPLIT_PARTS", "지출을 두 개 이상의 항목으로 나눠 주세요.");
        }
        long total;
        try {
            total = parts.stream().mapToLong(SplitPart::amountMinor).reduce(0, Math::addExact);
        } catch (ArithmeticException exception) {
            throw EarthTripException.badRequest("INVALID_SPLIT_TOTAL", "분할 금액 합계가 지원 범위를 벗어났습니다.");
        }
        if (total != original.amountMinor()) {
            throw EarthTripException.badRequest(
                    "INVALID_SPLIT_TOTAL", "분할 금액 합계가 원본 지출과 일치해야 합니다.");
        }
        if (parts.stream().map(SplitPart::requestId).distinct().count() != parts.size()) {
            throw EarthTripException.badRequest(
                    "DUPLICATE_SPLIT_REQUEST_ID", "분할 항목의 요청 ID는 서로 달라야 합니다.");
        }
    }

    private static boolean visible(Expense expense, UUID actorUserId) {
        return switch (expense.visibility()) {
            case "PRIVATE" -> expense.createdBy().equals(actorUserId);
            case "PARTICIPANTS" ->
                    expense.payers().containsKey(actorUserId)
                            || expense.shares().containsKey(actorUserId)
                            || expense.createdBy().equals(actorUserId);
            default -> true;
        };
    }

    private static void requireVisible(Expense expense, UUID actorUserId) {
        if (!visible(expense, actorUserId)) {
            throw notFound();
        }
    }

    private static long required(Long value) {
        if (value == null) {
            throw EarthTripException.badRequest("AMOUNT_REQUIRED", "금액이 필요합니다.");
        }
        return value;
    }

    private static void requireVersion(Expense expense, long baseVersion) {
        if (expense.version() != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 지출 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", expense.version()));
        }
    }

    private static ExpenseResult result(Expense expense) {
        return new ExpenseResult(
                expense.id(),
                expense.tripId(),
                expense.title(),
                expense.categoryCode(),
                expense.amountMinor(),
                expense.currency(),
                expense.occurredAt(),
                expense.payers(),
                expense.shares(),
                expense.visibility(),
                expense.status(),
                expense.note(),
                expense.version(),
                expense.createdBy(),
                expense.updatedBy(),
                expense.createdAt(),
                expense.updatedAt());
    }

    private static AdjustmentResult adjustment(ExpenseStorePort.AdjustmentRecord saved) {
        return new AdjustmentResult(
                saved.id(),
                saved.expenseId(),
                saved.kind(),
                saved.amountMinor(),
                saved.currency(),
                saved.participantId(),
                saved.payload(),
                saved.createdAt());
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("EXPENSE_NOT_FOUND", "지출을 찾을 수 없습니다.");
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 다른 지출에 사용된 요청 ID입니다.");
    }
}
