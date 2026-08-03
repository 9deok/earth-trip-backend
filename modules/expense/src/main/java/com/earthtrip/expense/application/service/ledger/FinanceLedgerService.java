package com.earthtrip.expense.application.service.ledger;

import com.earthtrip.expense.application.port.in.FinanceLedgerUseCase;
import com.earthtrip.expense.application.port.out.FinanceLedgerStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class FinanceLedgerService implements FinanceLedgerUseCase {

    private static final Set<String> MOVEMENT_TYPES = Set.of(
        "EXCHANGE_IN", "EXCHANGE_OUT", "WITHDRAWAL", "INCOME", "EXPENSE", "ADJUSTMENT"
    );
    private static final Set<String> NEGATIVE_MOVEMENT_TYPES = Set.of(
        "EXCHANGE_OUT", "EXPENSE"
    );
    private static final Set<String> STATUSES = Set.of("PENDING", "CONFIRMED", "VOIDED");

    private final TripAccess access;
    private final FinanceLedgerStorePort store;
    private final Clock clock;

    FinanceLedgerService(TripAccess access, FinanceLedgerStorePort store, Clock clock) {
        this.access = access;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashResult> cash(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return store.cash(tripId).stream().map(FinanceLedgerService::cashResult).toList();
    }

    @Override
    public CashResult createCash(UUID tripId, UUID actorUserId, CashCommand command) {
        access.requireEditor(tripId, actorUserId);
        FinanceLedgerStorePort.CashRecord existing = store.cashById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw idempotencyConflict();
            }
            return cashResult(existing);
        }
        String movementType = movementType(command.movementType());
        Instant now = clock.instant();
        return cashResult(store.saveCash(new FinanceLedgerStorePort.CashRecord(
            Objects.requireNonNull(command.requestId(), "요청 ID는 필수입니다."),
            tripId,
            movementType,
            signedAmount(command.amountMinor(), movementType),
            currency(command.currency()),
            command.payload() == null ? Map.of() : Map.copyOf(command.payload()),
            status(command.status()),
            requireOccurredAt(command.occurredAt()),
            0,
            actorUserId,
            now,
            now,
            null
        )));
    }

    @Override
    public CashResult updateCash(
        UUID tripId,
        UUID movementId,
        UUID actorUserId,
        CashCommand command
    ) {
        access.requireEditor(tripId, actorUserId);
        FinanceLedgerStorePort.CashRecord current = loadCash(tripId, movementId);
        requireVersion(current, command.baseVersion());
        String nextType = command.movementType() == null
            ? current.movementType()
            : movementType(command.movementType());
        long sourceAmount = command.amountMinor() == 0
            ? current.amountMinor()
            : command.amountMinor();
        FinanceLedgerStorePort.CashRecord updated = new FinanceLedgerStorePort.CashRecord(
            movementId,
            tripId,
            nextType,
            signedAmount(sourceAmount, nextType),
            command.currency() == null ? current.currency() : currency(command.currency()),
            command.payload() == null ? current.payload() : Map.copyOf(command.payload()),
            command.status() == null ? current.status() : status(command.status()),
            command.occurredAt() == null ? current.occurredAt() : command.occurredAt(),
            current.version(),
            current.createdBy(),
            current.createdAt(),
            clock.instant(),
            null
        );
        return cashResult(store.saveCash(updated));
    }

    @Override
    public void deleteCash(
        UUID tripId,
        UUID movementId,
        UUID actorUserId,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        FinanceLedgerStorePort.CashRecord current = loadCash(tripId, movementId);
        requireVersion(current, baseVersion);
        Instant now = clock.instant();
        store.saveCash(new FinanceLedgerStorePort.CashRecord(
            current.id(), current.tripId(), current.movementType(), current.amountMinor(),
            current.currency(), current.payload(), current.status(), current.occurredAt(),
            current.version(), current.createdBy(), current.createdAt(), now, now
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashBalance> balances(UUID tripId, UUID actorUserId) {
        List<CashResult> records = cash(tripId, actorUserId);
        Instant calculatedAt = clock.instant();
        return records.stream()
            .filter(record -> record.status().equals("CONFIRMED"))
            .collect(Collectors.groupingBy(
                CashResult::currency,
                TreeMap::new,
                Collectors.summingLong(CashResult::amountMinor)
            ))
            .entrySet().stream()
            .map(entry -> new CashBalance(entry.getKey(), entry.getValue(), calculatedAt))
            .toList();
    }

    @Override
    public CashResult reconcile(
        UUID tripId,
        UUID actorUserId,
        UUID requestId,
        String currency,
        long countedBalance,
        Map<String, Object> payload
    ) {
        String currencyCode = currency(currency);
        long estimated = balances(tripId, actorUserId).stream()
            .filter(balance -> balance.currency().equals(currencyCode))
            .mapToLong(CashBalance::estimatedBalance)
            .findFirst()
            .orElse(0);
        long difference;
        try {
            difference = Math.subtractExact(countedBalance, estimated);
        } catch (ArithmeticException overflow) {
            throw EarthTripException.badRequest(
                "CASH_BALANCE_OVERFLOW",
                "현금 잔액 차이가 지원 범위를 벗어났습니다."
            );
        }
        return createCash(tripId, actorUserId, new CashCommand(
            requestId, "ADJUSTMENT", difference, currencyCode,
            payload, "CONFIRMED", clock.instant(), 0
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateResult> rates(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return store.rates(tripId).stream().map(FinanceLedgerService::rateResult).toList();
    }

    @Override
    public RateResult createRate(UUID tripId, UUID actorUserId, RateCommand command) {
        access.requireEditor(tripId, actorUserId);
        if (command.rate() == null || command.rate().signum() <= 0) {
            throw EarthTripException.badRequest(
                "INVALID_EXCHANGE_RATE",
                "환율은 0보다 커야 합니다."
            );
        }
        FinanceLedgerStorePort.RateRecord existing = store.rateById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw idempotencyConflict();
            }
            return rateResult(existing);
        }
        String baseCurrency = currency(command.baseCurrency());
        String quoteCurrency = currency(command.quoteCurrency());
        if (baseCurrency.equals(quoteCurrency)) {
            throw EarthTripException.badRequest(
                "SAME_EXCHANGE_CURRENCY",
                "기준 통화와 상대 통화는 달라야 합니다."
            );
        }
        Instant now = clock.instant();
        return rateResult(store.saveRate(new FinanceLedgerStorePort.RateRecord(
            Objects.requireNonNull(command.requestId(), "요청 ID는 필수입니다."),
            tripId,
            baseCurrency,
            quoteCurrency,
            command.rate(),
            command.source() == null ? "MANUAL" : command.source().strip().toUpperCase(Locale.ROOT),
            Objects.requireNonNull(command.observedAt(), "환율 관측 시각은 필수입니다."),
            actorUserId,
            now
        )));
    }

    private FinanceLedgerStorePort.CashRecord loadCash(UUID tripId, UUID movementId) {
        return store.cashById(movementId)
            .filter(record -> record.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "CASH_MOVEMENT_NOT_FOUND",
                "현금 원장 항목을 찾을 수 없습니다."
            ));
    }

    private static String movementType(String value) {
        if (value == null) {
            throw EarthTripException.badRequest(
                "CASH_MOVEMENT_TYPE_REQUIRED",
                "현금 원장 유형이 필요합니다."
            );
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!MOVEMENT_TYPES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_CASH_MOVEMENT_TYPE",
                "지원하지 않는 현금 원장 유형입니다."
            );
        }
        return normalized;
    }

    private static long signedAmount(long value, String movementType) {
        if (value == 0 && !movementType.equals("ADJUSTMENT")) {
            throw EarthTripException.badRequest(
                "INVALID_CASH_AMOUNT",
                "현금 원장 금액은 0일 수 없습니다."
            );
        }
        if (movementType.equals("ADJUSTMENT")) {
            return value;
        }
        if (value == Long.MIN_VALUE) {
            throw EarthTripException.badRequest(
                "INVALID_CASH_AMOUNT",
                "현금 원장 금액이 지원 범위를 벗어났습니다."
            );
        }
        long absolute = Math.abs(value);
        return NEGATIVE_MOVEMENT_TYPES.contains(movementType) ? -absolute : absolute;
    }

    private static String currency(String value) {
        if (value == null) {
            throw EarthTripException.badRequest("CURRENCY_REQUIRED", "통화 코드가 필요합니다.");
        }
        try {
            return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw EarthTripException.badRequest(
                "INVALID_CURRENCY",
                "유효한 ISO 4217 통화 코드가 아닙니다."
            );
        }
    }

    private static String status(String value) {
        String normalized = value == null ? "CONFIRMED" : value.strip().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw EarthTripException.badRequest(
                "INVALID_CASH_STATUS",
                "지원하지 않는 현금 원장 상태입니다."
            );
        }
        return normalized;
    }

    private static Instant requireOccurredAt(Instant occurredAt) {
        if (occurredAt == null) {
            throw EarthTripException.badRequest(
                "OCCURRED_AT_REQUIRED",
                "현금 변동 시각이 필요합니다."
            );
        }
        return occurredAt;
    }

    private static CashResult cashResult(FinanceLedgerStorePort.CashRecord record) {
        return new CashResult(
            record.id(), record.tripId(), record.movementType(), record.amountMinor(),
            record.currency(), record.payload(), record.status(), record.occurredAt(),
            record.version(), record.createdBy(), record.createdAt(), record.updatedAt()
        );
    }

    private static RateResult rateResult(FinanceLedgerStorePort.RateRecord record) {
        return new RateResult(
            record.id(), record.baseCurrency(), record.quoteCurrency(), record.rate(),
            record.source(), record.observedAt(), record.createdAt()
        );
    }

    private static void requireVersion(
        FinanceLedgerStorePort.CashRecord record,
        long baseVersion
    ) {
        if (record.version() != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 현금 원장 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", record.version())
            );
        }
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict(
            "IDEMPOTENCY_KEY_REUSED",
            "이미 다른 작업에 사용된 요청 ID입니다."
        );
    }
}
