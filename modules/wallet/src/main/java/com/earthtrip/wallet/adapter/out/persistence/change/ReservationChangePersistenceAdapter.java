package com.earthtrip.wallet.adapter.out.persistence.change;

import com.earthtrip.wallet.application.port.out.ReservationChangeStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReservationChangePersistenceAdapter implements ReservationChangeStorePort {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final ReservationChangeSetJpaRepository repository;
    private final ObjectMapper json;

    ReservationChangePersistenceAdapter(
            ReservationChangeSetJpaRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public Optional<ChangeSetRecord> find(UUID changeSetId) {
        return repository.findById(changeSetId.toString()).map(this::record);
    }

    @Override
    public ChangeSetRecord save(ChangeSetRecord record) {
        return record(
                repository.save(
                        new ReservationChangeSetJpaEntity(
                                record,
                                write(record.beforeSnapshot()),
                                write(record.afterSnapshot()))));
    }

    private ChangeSetRecord record(ReservationChangeSetJpaEntity entity) {
        return entity.toRecord(read(entity.beforeSnapshot()), read(entity.afterSnapshot()));
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("예약 변경 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 예약 변경 스냅샷을 읽을 수 없습니다.", exception);
        }
    }
}
