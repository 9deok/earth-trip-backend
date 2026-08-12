package com.earthtrip.expense.adapter.out.persistence.ledger;

import com.earthtrip.expense.application.port.out.FinanceLedgerStorePort;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class FinanceLedgerPersistenceAdapter implements FinanceLedgerStorePort {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final CashMovementJpaRepository cash;
    private final ExchangeRateJpaRepository rates;
    private final ObjectMapper json;

    FinanceLedgerPersistenceAdapter(
            CashMovementJpaRepository c, ExchangeRateJpaRepository r, ObjectMapper j) {
        cash = c;
        rates = r;
        json = j;
    }

    @Override
    public List<CashRecord> cash(UUID trip) {
        return cash.findAllByTripIdAndDeletedAtIsNullOrderByOccurredAtAsc(trip.toString()).stream()
                .map(this::record)
                .toList();
    }

    @Override
    public Optional<CashRecord> cashById(UUID id) {
        return cash.findById(id.toString()).map(this::record).filter(r -> r.deletedAt() == null);
    }

    @Override
    public CashRecord saveCash(CashRecord r) {
        String data = write(r.payload());
        CashMovementJpaEntity e =
                cash.findById(r.id().toString())
                        .map(
                                x -> {
                                    x.apply(r, data);
                                    return x;
                                })
                        .orElseGet(() -> new CashMovementJpaEntity(r, data));
        return record(cash.saveAndFlush(e));
    }

    @Override
    public List<RateRecord> rates(UUID trip) {
        return rates.findAllByTripIdOrderByObservedAtDesc(trip.toString()).stream()
                .map(ExchangeRateJpaEntity::record)
                .toList();
    }

    @Override
    public Optional<RateRecord> rateById(UUID id) {
        return rates.findById(id.toString()).map(ExchangeRateJpaEntity::record);
    }

    @Override
    public RateRecord saveRate(RateRecord r) {
        return rates.save(new ExchangeRateJpaEntity(r)).record();
    }

    private CashRecord record(CashMovementJpaEntity e) {
        try {
            return e.record(json.readValue(e.payload(), MAP));
        } catch (JsonProcessingException x) {
            throw new IllegalStateException("현금 원장 JSON을 읽을 수 없습니다.", x);
        }
    }

    private String write(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException x) {
            throw new IllegalArgumentException("JSON으로 저장할 수 없습니다.", x);
        }
    }
}
