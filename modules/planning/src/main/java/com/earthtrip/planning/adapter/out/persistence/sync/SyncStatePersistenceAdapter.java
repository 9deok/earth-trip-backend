package com.earthtrip.planning.adapter.out.persistence.sync;

import com.earthtrip.planning.application.port.out.SyncStateStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SyncStatePersistenceAdapter implements SyncStateStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };

    private final ActivityReadCursorJpaRepository cursors;
    private final SyncConflictJpaRepository conflicts;
    private final ObjectMapper json;

    SyncStatePersistenceAdapter(
        ActivityReadCursorJpaRepository cursors,
        SyncConflictJpaRepository conflicts,
        ObjectMapper json
    ) {
        this.cursors = cursors;
        this.conflicts = conflicts;
        this.json = json;
    }

    @Override
    public long readCursor(UUID tripId, UUID userId) {
        return cursors.findById(new ActivityReadCursorId(
            tripId.toString(), userId.toString()
        )).map(ActivityReadCursorJpaEntity::toRecord)
            .map(ReadCursorRecord::sequenceId)
            .orElse(0L);
    }

    @Override
    public ReadCursorRecord saveReadCursor(ReadCursorRecord record) {
        ActivityReadCursorId id = new ActivityReadCursorId(
            record.tripId().toString(), record.userId().toString()
        );
        ActivityReadCursorJpaEntity entity = cursors.findById(id)
            .map(existing -> {
                existing.apply(record);
                return existing;
            })
            .orElseGet(() -> new ActivityReadCursorJpaEntity(record));
        return cursors.save(entity).toRecord();
    }

    @Override
    public List<ConflictRecord> findOpenConflicts(UUID tripId) {
        return conflicts.findAllByTripIdAndStatusOrderByCreatedAtAsc(
            tripId.toString(), "OPEN"
        ).stream().map(this::conflict).toList();
    }

    @Override
    public Optional<ConflictRecord> findConflict(UUID conflictId) {
        return conflicts.findById(conflictId.toString()).map(this::conflict);
    }

    @Override
    public ConflictRecord saveConflict(ConflictRecord record) {
        String device = write(record.deviceCommand());
        String server = record.serverSnapshot() == null
            ? null
            : write(record.serverSnapshot());
        String fields = write(record.mergeableFields());
        SyncConflictJpaEntity entity = conflicts.findById(record.conflictId().toString())
            .map(existing -> {
                existing.apply(record, device, server, fields);
                return existing;
            })
            .orElseGet(() -> new SyncConflictJpaEntity(record, device, server, fields));
        return conflict(conflicts.saveAndFlush(entity));
    }

    private ConflictRecord conflict(SyncConflictJpaEntity entity) {
        return new ConflictRecord(
            entity.id(), entity.operationId(), entity.tripId(), entity.actorId(),
            entity.action(), entity.resourceType(), entity.resourceId(),
            readMap(entity.deviceCommand()),
            entity.serverSnapshot() == null ? null : readMap(entity.serverSnapshot()),
            readStrings(entity.mergeableFields()), entity.status(), entity.resolution(),
            entity.createdAt(), entity.resolvedAt(), entity.version()
        );
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("동기화 상태를 JSON으로 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 동기화 상태를 읽을 수 없습니다.", exception);
        }
    }

    private List<String> readStrings(String value) {
        try {
            return json.readValue(value, STRINGS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 병합 필드를 읽을 수 없습니다.", exception);
        }
    }
}
