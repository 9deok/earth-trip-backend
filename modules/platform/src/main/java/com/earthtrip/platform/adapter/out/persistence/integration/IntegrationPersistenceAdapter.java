package com.earthtrip.platform.adapter.out.persistence.integration;

import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class IntegrationPersistenceAdapter implements IntegrationStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<Set<String>> SET = new TypeReference<>() { };

    private final IntegrationConnectionJpaRepository connections;
    private final IntegrationSyncJpaRepository syncs;
    private final InboundAliasJpaRepository aliases;
    private final CalendarSyncJpaRepository calendars;
    private final ObjectMapper json;

    IntegrationPersistenceAdapter(
        IntegrationConnectionJpaRepository connections,
        IntegrationSyncJpaRepository syncs,
        InboundAliasJpaRepository aliases,
        CalendarSyncJpaRepository calendars,
        ObjectMapper json
    ) {
        this.connections = connections;
        this.syncs = syncs;
        this.aliases = aliases;
        this.calendars = calendars;
        this.json = json;
    }

    @Override
    public List<ConnectionRecord> connections(UUID userId, String kind) {
        return connections.findAllByUserIdAndKindAndRevokedAtIsNullOrderByCreatedAtDesc(
            userId.toString(),
            kind
        ).stream().map(this::connection).toList();
    }

    @Override
    public Optional<ConnectionRecord> connection(UUID id) {
        return connections.findById(id.toString()).map(this::connection);
    }

    @Override
    public ConnectionRecord saveConnection(ConnectionRecord record) {
        String scopes = write(record.scopes());
        String metadata = write(record.metadata());
        IntegrationConnectionJpaEntity entity = connections.findById(record.id().toString())
            .map(existing -> {
                existing.apply(record, scopes, metadata);
                return existing;
            })
            .orElseGet(() -> new IntegrationConnectionJpaEntity(record, scopes, metadata));
        return connection(connections.saveAndFlush(entity));
    }

    @Override
    public Optional<SyncRecord> sync(UUID id) {
        return syncs.findById(id.toString()).map(this::sync);
    }

    @Override
    public SyncRecord saveSync(SyncRecord record) {
        String request = write(record.request());
        String result = write(record.result());
        IntegrationSyncJpaEntity entity = syncs.findById(record.id().toString())
            .map(existing -> {
                existing.apply(record, request, result);
                return existing;
            })
            .orElseGet(() -> new IntegrationSyncJpaEntity(record, request, result));
        return sync(syncs.saveAndFlush(entity));
    }

    @Override
    public List<AliasRecord> aliases(UUID userId) {
        return aliases.findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(
            userId.toString()
        ).stream().map(InboundAliasJpaEntity::record).toList();
    }

    @Override
    public Optional<AliasRecord> alias(UUID id) {
        return aliases.findById(id.toString()).map(InboundAliasJpaEntity::record);
    }

    @Override
    public Optional<AliasRecord> aliasByAddress(String alias) {
        return aliases.findByAliasAndRevokedAtIsNull(alias)
            .map(InboundAliasJpaEntity::record);
    }

    @Override
    public AliasRecord saveAlias(AliasRecord record) {
        InboundAliasJpaEntity entity = aliases.findById(record.id().toString())
            .map(existing -> {
                existing.apply(record);
                return existing;
            })
            .orElseGet(() -> new InboundAliasJpaEntity(record));
        return aliases.saveAndFlush(entity).record();
    }

    @Override
    public Optional<CalendarRecord> calendar(UUID tripId) {
        return calendars.findById(tripId.toString()).map(this::calendar);
    }

    @Override
    public CalendarRecord saveCalendar(CalendarRecord record) {
        String scope = write(record.scopeConfig());
        CalendarSyncJpaEntity entity = calendars.findById(record.tripId().toString())
            .map(existing -> {
                existing.apply(record, scope);
                return existing;
            })
            .orElseGet(() -> new CalendarSyncJpaEntity(record, scope));
        return calendar(calendars.saveAndFlush(entity));
    }

    @Override
    public void deleteCalendar(UUID tripId) {
        calendars.deleteById(tripId.toString());
    }

    private ConnectionRecord connection(IntegrationConnectionJpaEntity entity) {
        return entity.record(read(entity.scopes, SET), read(entity.metadata, MAP));
    }

    private SyncRecord sync(IntegrationSyncJpaEntity entity) {
        return entity.record(read(entity.request, MAP), read(entity.result, MAP));
    }

    private CalendarRecord calendar(CalendarSyncJpaEntity entity) {
        return entity.record(read(entity.scope, MAP));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("연동 JSON을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 연동 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
