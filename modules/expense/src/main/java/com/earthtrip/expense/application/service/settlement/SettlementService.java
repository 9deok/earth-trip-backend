package com.earthtrip.expense.application.service.settlement;

import com.earthtrip.expense.application.port.in.SettlementUseCase;
import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.application.port.out.SettlementStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SettlementService implements SettlementUseCase {

    private final TripAccess access;
    private final ExpenseStorePort expenses;
    private final SettlementStorePort settlements;
    private final SettlementPreviewCalculator previewCalculator;
    private final Clock clock;

    SettlementService(
            TripAccess access,
            ExpenseStorePort expenses,
            SettlementStorePort settlements,
            Clock clock) {
        this.access = access;
        this.expenses = expenses;
        this.settlements = settlements;
        this.previewCalculator = new SettlementPreviewCalculator(expenses);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(
            UUID tripId, UUID actorUserId, String baseCurrency, Map<String, BigDecimal> rates) {
        access.requireViewer(tripId, actorUserId);
        return previewCalculator.calculate(tripId, baseCurrency, rates);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return settlements.findAll(tripId).stream().map(SettlementService::result).toList();
    }

    @Override
    public SettlementResult create(
            UUID tripId,
            UUID actorUserId,
            UUID requestId,
            String baseCurrency,
            Map<String, BigDecimal> rates) {
        access.requireEditor(tripId, actorUserId);
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        SettlementStorePort.SettlementRecord existing =
                settlements.findById(requestId).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw idempotencyConflict();
            }
            return result(existing);
        }
        Instant now = clock.instant();
        PreviewResult snapshot = preview(tripId, actorUserId, baseCurrency, rates);
        return result(
                settlements.save(
                        new SettlementStorePort.SettlementRecord(
                                requestId,
                                tripId,
                                snapshot.baseCurrency(),
                                "DRAFT",
                                snapshot,
                                0,
                                actorUserId,
                                now,
                                now,
                                null)));
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResult get(UUID tripId, UUID settlementId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return result(load(tripId, settlementId));
    }

    @Override
    public SettlementResult update(
            UUID tripId,
            UUID settlementId,
            UUID actorUserId,
            String baseCurrency,
            Map<String, BigDecimal> minorUnitRates,
            long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        SettlementStorePort.SettlementRecord current = load(tripId, settlementId);
        version(current, baseVersion);
        requireOpen(current);
        String nextBase = baseCurrency == null ? current.baseCurrency() : baseCurrency;
        Map<String, BigDecimal> nextRates =
                minorUnitRates == null ? current.snapshot().minorUnitRates() : minorUnitRates;
        PreviewResult snapshot = preview(tripId, actorUserId, nextBase, nextRates);
        return result(
                settlements.save(
                        new SettlementStorePort.SettlementRecord(
                                current.id(),
                                current.tripId(),
                                snapshot.baseCurrency(),
                                current.status(),
                                snapshot,
                                current.version(),
                                current.createdBy(),
                                current.createdAt(),
                                clock.instant(),
                                current.closedAt())));
    }

    @Override
    public SettlementResult close(
            UUID tripId,
            UUID settlementId,
            UUID actorUserId,
            long baseVersion,
            boolean paymentsConfirmed) {
        access.requireEditor(tripId, actorUserId);
        if (!paymentsConfirmed) {
            throw EarthTripException.badRequest("PAYMENTS_CONFIRMATION_REQUIRED", "송금 확인이 필요합니다.");
        }
        SettlementStorePort.SettlementRecord settlement = load(tripId, settlementId);
        version(settlement, baseVersion);
        if (!List.of("DRAFT", "REOPENED").contains(settlement.status())) {
            throw EarthTripException.conflict("SETTLEMENT_NOT_CLOSABLE", "열린 정산만 마감할 수 있습니다.");
        }
        Instant now = clock.instant();
        return result(settlements.save(copy(settlement, "CLOSED", now, now)));
    }

    @Override
    public SettlementResult reopen(
            UUID tripId, UUID settlementId, UUID actorUserId, long baseVersion, String reason) {
        access.requireEditor(tripId, actorUserId);
        if (reason == null || reason.isBlank() || reason.strip().length() > 1_000) {
            throw EarthTripException.badRequest(
                    "SETTLEMENT_REOPEN_REASON_REQUIRED", "1000자 이하의 다시 열기 사유가 필요합니다.");
        }
        SettlementStorePort.SettlementRecord settlement = load(tripId, settlementId);
        version(settlement, baseVersion);
        if (!settlement.status().equals("CLOSED")) {
            throw EarthTripException.conflict("SETTLEMENT_NOT_CLOSED", "마감된 정산만 다시 열 수 있습니다.");
        }
        return result(
                settlements.save(
                        copy(settlement, "REOPENED", clock.instant(), settlement.closedAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResult> payments(UUID tripId, UUID settlementId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        load(tripId, settlementId);
        return settlements.payments(settlementId).stream().map(SettlementService::payment).toList();
    }

    @Override
    public PaymentResult createPayment(
            UUID tripId, UUID settlementId, UUID actorUserId, PaymentCommand command) {
        access.requireEditor(tripId, actorUserId);
        SettlementStorePort.SettlementRecord settlement = load(tripId, settlementId);
        requireOpen(settlement);
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        SettlementStorePort.PaymentRecord existing =
                settlements.payment(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.settlementId().equals(settlementId)) {
                throw idempotencyConflict();
            }
            return payment(existing);
        }
        validatePayment(command);
        Instant now = clock.instant();
        return payment(
                settlements.savePayment(
                        new SettlementStorePort.PaymentRecord(
                                command.requestId(),
                                settlementId,
                                command.fromUserId(),
                                command.toUserId(),
                                command.amountMinor(),
                                currency(command.currency()),
                                command.paidAt(),
                                note(command.note()),
                                0,
                                now,
                                now)));
    }

    @Override
    public PaymentResult updatePayment(
            UUID tripId,
            UUID settlementId,
            UUID paymentId,
            UUID actorUserId,
            PaymentCommand command) {
        access.requireEditor(tripId, actorUserId);
        requireOpen(load(tripId, settlementId));
        SettlementStorePort.PaymentRecord current = loadPayment(settlementId, paymentId);
        if (current.version() != command.baseVersion()) {
            throw conflict(current.version());
        }
        validatePayment(command);
        return payment(
                settlements.savePayment(
                        new SettlementStorePort.PaymentRecord(
                                paymentId,
                                settlementId,
                                command.fromUserId(),
                                command.toUserId(),
                                command.amountMinor(),
                                currency(command.currency()),
                                command.paidAt(),
                                note(command.note()),
                                current.version(),
                                current.createdAt(),
                                clock.instant())));
    }

    @Override
    public void deletePayment(
            UUID tripId, UUID settlementId, UUID paymentId, UUID actorUserId, long baseVersion) {
        access.requireEditor(tripId, actorUserId);
        requireOpen(load(tripId, settlementId));
        SettlementStorePort.PaymentRecord payment = loadPayment(settlementId, paymentId);
        if (payment.version() != baseVersion) {
            throw conflict(payment.version());
        }
        settlements.deletePayment(paymentId);
    }

    private static UUID payerId(
            ExpenseStorePort.AdjustmentRecord adjustment, Map<UUID, Long> payers) {
        Object value = adjustment.payload().get("payerUserId");
        if (value != null) {
            try {
                UUID payerId = UUID.fromString(String.valueOf(value));
                if (payers.containsKey(payerId)) {
                    return payerId;
                }
            } catch (IllegalArgumentException ignored) {
                // A malformed optional hint falls back to deterministic payer allocation.
            }
        }
        return null;
    }

    private static void reduce(Map<UUID, Long> amounts, long reduction, UUID preferred) {
        long remaining = reduction;
        if (preferred != null && amounts.containsKey(preferred)) {
            remaining = reduceOne(amounts, preferred, remaining);
        }
        for (UUID userId : amounts.keySet().stream().sorted().toList()) {
            if (remaining == 0) {
                break;
            }
            if (!userId.equals(preferred)) {
                remaining = reduceOne(amounts, userId, remaining);
            }
        }
        if (remaining != 0) {
            throw new IllegalStateException("환불 금액을 분담 정보에 반영할 수 없습니다.");
        }
    }

    private static long reduceOne(Map<UUID, Long> amounts, UUID userId, long remaining) {
        long current = amounts.get(userId);
        long applied = Math.min(current, remaining);
        amounts.put(userId, current - applied);
        return remaining - applied;
    }

    private SettlementStorePort.SettlementRecord load(UUID tripId, UUID settlementId) {
        return settlements
                .findById(settlementId)
                .filter(settlement -> settlement.tripId().equals(tripId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "SETTLEMENT_NOT_FOUND", "정산을 찾을 수 없습니다."));
    }

    private SettlementStorePort.PaymentRecord loadPayment(UUID settlementId, UUID paymentId) {
        return settlements
                .payment(paymentId)
                .filter(payment -> payment.settlementId().equals(settlementId))
                .orElseThrow(
                        () ->
                                EarthTripException.notFound(
                                        "PAYMENT_NOT_FOUND", "송금 기록을 찾을 수 없습니다."));
    }

    private static void validatePayment(PaymentCommand command) {
        if (command.fromUserId() == null
                || command.toUserId() == null
                || command.fromUserId().equals(command.toUserId())
                || command.amountMinor() <= 0) {
            throw EarthTripException.badRequest("INVALID_PAYMENT", "송금 정보를 확인해 주세요.");
        }
    }

    private static String note(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.strip().length() > 500) {
            throw EarthTripException.badRequest("PAYMENT_NOTE_TOO_LONG", "송금 메모는 500자 이하여야 합니다.");
        }
        return value.strip();
    }

    private static String currency(String value) {
        try {
            return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (RuntimeException exception) {
            throw EarthTripException.badRequest("INVALID_CURRENCY", "유효한 ISO 4217 통화 코드가 아닙니다.");
        }
    }

    private static SettlementResult result(SettlementStorePort.SettlementRecord settlement) {
        return new SettlementResult(
                settlement.id(),
                settlement.tripId(),
                settlement.baseCurrency(),
                settlement.status(),
                settlement.snapshot(),
                settlement.version(),
                settlement.createdBy(),
                settlement.createdAt(),
                settlement.updatedAt(),
                settlement.closedAt());
    }

    private static PaymentResult payment(SettlementStorePort.PaymentRecord payment) {
        return new PaymentResult(
                payment.id(),
                payment.settlementId(),
                payment.fromUserId(),
                payment.toUserId(),
                payment.amountMinor(),
                payment.currency(),
                payment.paidAt(),
                payment.note(),
                payment.version(),
                payment.createdAt(),
                payment.updatedAt());
    }

    private static SettlementStorePort.SettlementRecord copy(
            SettlementStorePort.SettlementRecord settlement,
            String status,
            Instant updatedAt,
            Instant closedAt) {
        return new SettlementStorePort.SettlementRecord(
                settlement.id(),
                settlement.tripId(),
                settlement.baseCurrency(),
                status,
                settlement.snapshot(),
                settlement.version(),
                settlement.createdBy(),
                settlement.createdAt(),
                updatedAt,
                closedAt);
    }

    private static void requireOpen(SettlementStorePort.SettlementRecord settlement) {
        if (settlement.status().equals("CLOSED")) {
            throw EarthTripException.conflict("SETTLEMENT_CLOSED", "마감된 정산은 수정할 수 없습니다.");
        }
    }

    private static void version(SettlementStorePort.SettlementRecord settlement, long baseVersion) {
        if (settlement.version() != baseVersion) {
            throw conflict(settlement.version());
        }
    }

    private static long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw EarthTripException.badRequest(
                    "SETTLEMENT_AMOUNT_OVERFLOW", "정산 금액 합계가 지원 범위를 벗어났습니다.");
        }
    }

    private static EarthTripException conflict(long serverVersion) {
        return new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 정산 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", serverVersion));
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED", "이미 다른 정산 작업에 사용된 요청 ID입니다.");
    }
}
