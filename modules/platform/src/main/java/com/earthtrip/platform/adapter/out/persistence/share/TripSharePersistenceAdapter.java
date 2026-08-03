package com.earthtrip.platform.adapter.out.persistence.share;

import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripSharePersistenceAdapter implements TripShareStorePort {
    private static final TypeReference<List<String>> SCOPES = new TypeReference<>() { };
    private final TripShareLinkJpaRepository links;
    private final TripSharePasswordSessionJpaRepository sessions;
    private final TripShareAccessEventJpaRepository events;
    private final ObjectMapper json;
    TripSharePersistenceAdapter(
        TripShareLinkJpaRepository links,
        TripSharePasswordSessionJpaRepository sessions,
        TripShareAccessEventJpaRepository events,
        ObjectMapper json
    ) { this.links = links; this.sessions = sessions; this.events = events; this.json = json; }
    @Override public List<ShareRecord> findAll(UUID tripId) {
        return links.findAllByTripIdOrderByCreatedAtDesc(tripId.toString()).stream()
            .map(this::share).toList();
    }
    @Override public Optional<ShareRecord> findById(UUID shareId) {
        return links.findById(shareId.toString()).map(this::share);
    }
    @Override public Optional<ShareRecord> findByTokenHash(String tokenHash) {
        return links.findByTokenHash(tokenHash).map(this::share);
    }
    @Override public ShareRecord save(ShareRecord record) {
        String scopes = write(record.scopes());
        TripShareLinkJpaEntity entity = links.findById(record.id().toString())
            .map(existing -> { existing.apply(record, scopes); return existing; })
            .orElseGet(() -> new TripShareLinkJpaEntity(record, scopes));
        return share(links.saveAndFlush(entity));
    }
    @Override public Optional<PasswordSessionRecord> findPasswordSession(String tokenHash) {
        return sessions.findById(tokenHash).map(TripSharePasswordSessionJpaEntity::toRecord);
    }
    @Override public PasswordSessionRecord savePasswordSession(PasswordSessionRecord record) {
        return sessions.save(new TripSharePasswordSessionJpaEntity(record)).toRecord();
    }
    @Override public AccessRecord appendAccess(AccessRecord record) {
        return events.save(new TripShareAccessEventJpaEntity(record)).toRecord();
    }
    @Override public List<AccessRecord> accessEvents(UUID shareId) {
        return events.findAllByShareIdOrderByOccurredAtDesc(shareId.toString()).stream()
            .map(TripShareAccessEventJpaEntity::toRecord).toList();
    }
    private ShareRecord share(TripShareLinkJpaEntity entity) {
        try { return entity.toRecord(json.readValue(entity.scopes(), SCOPES)); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 공유 범위를 읽을 수 없습니다.", exception);
        }
    }
    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("공유 범위를 저장할 수 없습니다.", exception);
        }
    }
}
