package com.earthtrip.wallet.adapter.out.persistence.record;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.wallet.application.port.out.SensitiveWalletDataPort;
import com.earthtrip.wallet.application.port.out.WalletRecordStorePort;
import com.earthtrip.wallet.domain.WalletRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class WalletRecordPersistenceAdapter implements WalletRecordStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final Set<String> SENSITIVE_FIELDS =
            Set.of(
                    "confirmationNumber",
                    "confirmationCode",
                    "bookingReference",
                    "reservationNumber",
                    "passengerNames",
                    "personalNote");

    private final WalletRecordJpaRepository repository;
    private final SensitiveWalletDataPort sensitiveData;
    private final ObjectMapper json;

    WalletRecordPersistenceAdapter(
            WalletRecordJpaRepository repository,
            SensitiveWalletDataPort sensitiveData,
            ObjectMapper json) {
        this.repository = repository;
        this.sensitiveData = sensitiveData;
        this.json = json;
    }

    @Override
    public List<WalletRecord> findAll(UUID tripId, String type, UUID parentId) {
        List<WalletRecordJpaEntity> rows =
                parentId == null
                        ? repository
                                .findAllByTripIdAndTypeAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(
                                        tripId.toString(), type)
                        : repository
                                .findAllByTripIdAndTypeAndParentIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(
                                        tripId.toString(), type, parentId.toString());
        return rows.stream().map(this::domain).toList();
    }

    @Override
    public Optional<WalletRecord> findById(UUID id) {
        return repository
                .findById(id.toString())
                .map(this::domain)
                .filter(record -> record.deletedAt() == null);
    }

    @Override
    public WalletRecord save(WalletRecord record) {
        String payload = write(protectMap(record.payload()));
        WalletRecordJpaEntity entity =
                repository
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, payload);
                                    return existing;
                                })
                        .orElseGet(() -> new WalletRecordJpaEntity(record, payload));
        return domain(repository.saveAndFlush(entity));
    }

    private WalletRecord domain(WalletRecordJpaEntity entity) {
        try {
            return entity.toDomain(revealMap(json.readValue(entity.payload(), MAP)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 지갑 JSON을 읽을 수 없습니다.", exception);
        }
    }

    private Map<String, Object> protectMap(Map<String, Object> source) {
        Map<String, Object> protectedValues = new LinkedHashMap<>();
        source.forEach(
                (key, value) ->
                        protectedValues.put(
                                key,
                                SENSITIVE_FIELDS.contains(key)
                                        ? sensitiveData.protect(key, value)
                                        : protectNested(value)));
        return protectedValues;
    }

    private Object protectNested(Object value) {
        if (sensitiveData.isProtected(value)) {
            throw EarthTripException.badRequest(
                    "SENSITIVE_VALUE_ALREADY_PROTECTED", "암호화 envelope를 API 값으로 직접 저장할 수 없습니다.");
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, nested) -> normalized.put(String.valueOf(key), nested));
            return protectMap(normalized);
        }
        if (value instanceof List<?> list) {
            List<Object> protectedValues = new ArrayList<>(list.size());
            list.forEach(item -> protectedValues.add(protectNested(item)));
            return protectedValues;
        }
        return value;
    }

    private Map<String, Object> revealMap(Map<String, Object> source) {
        Map<String, Object> revealed = new LinkedHashMap<>();
        source.forEach((key, value) -> revealed.put(key, revealNested(key, value)));
        return revealed;
    }

    private Object revealNested(String fieldName, Object value) {
        if (sensitiveData.isProtected(value)) {
            return sensitiveData.reveal(fieldName, value);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach(
                    (key, nested) ->
                            normalized.put(
                                    String.valueOf(key),
                                    revealNested(String.valueOf(key), nested)));
            return normalized;
        }
        if (value instanceof List<?> list) {
            List<Object> revealed = new ArrayList<>(list.size());
            list.forEach(item -> revealed.add(revealNested(fieldName, item)));
            return revealed;
        }
        return value;
    }

    private String write(Map<String, Object> data) {
        try {
            return json.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("저장할 수 없는 지갑 JSON입니다.", exception);
        }
    }
}
