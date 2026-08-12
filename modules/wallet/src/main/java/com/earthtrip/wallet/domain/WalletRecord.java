package com.earthtrip.wallet.domain;

import java.time.Instant;
import java.util.*;

public final class WalletRecord {
    private final UUID id;
    private final UUID tripId;
    private final String type;
    private final UUID parentId;
    private Map<String, Object> payload;
    private String status;
    private String visibility;
    private int sortOrder;
    private final UUID createdBy;
    private UUID updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final long version;

    private WalletRecord(
            UUID id,
            UUID trip,
            String type,
            UUID parent,
            Map<String, Object> data,
            String status,
            String visibility,
            int order,
            UUID createdBy,
            UUID updatedBy,
            Instant created,
            Instant updated,
            Instant deleted,
            long version) {
        this.id = Objects.requireNonNull(id);
        tripId = Objects.requireNonNull(trip);
        this.type = text(type, 40);
        parentId = parent;
        this.createdBy = createdBy;
        createdAt = created;
        this.version = version;
        apply(data, status, visibility, order, updatedBy, updated);
        deletedAt = deleted;
    }

    public static WalletRecord create(
            UUID id,
            UUID trip,
            String type,
            UUID parent,
            Map<String, Object> data,
            String status,
            String visibility,
            int order,
            UUID actor,
            Instant now) {
        return new WalletRecord(
                id,
                trip,
                type,
                parent,
                data,
                status,
                visibility,
                order,
                actor,
                actor,
                now,
                now,
                null,
                0);
    }

    public static WalletRecord restore(
            UUID id,
            UUID trip,
            String type,
            UUID parent,
            Map<String, Object> data,
            String status,
            String visibility,
            int order,
            UUID createdBy,
            UUID updatedBy,
            Instant created,
            Instant updated,
            Instant deleted,
            long version) {
        return new WalletRecord(
                id,
                trip,
                type,
                parent,
                data,
                status,
                visibility,
                order,
                createdBy,
                updatedBy,
                created,
                updated,
                deleted,
                version);
    }

    public void update(
            Map<String, Object> data,
            String state,
            String scope,
            Integer order,
            UUID actor,
            Instant now) {
        apply(
                data == null ? payload : data,
                state == null ? status : state,
                scope == null ? visibility : scope,
                order == null ? sortOrder : order,
                actor,
                now);
    }

    public void delete(UUID actor, Instant now) {
        deletedAt = now;
        updatedBy = actor;
        updatedAt = now;
    }

    private void apply(
            Map<String, Object> data,
            String state,
            String scope,
            int order,
            UUID actor,
            Instant now) {
        payload = immutablePayload(data);
        status = text(state, 40).toUpperCase(Locale.ROOT);
        visibility = text(scope, 20).toUpperCase(Locale.ROOT);
        if (!Set.of("PRIVATE", "PARTICIPANTS", "TRIP").contains(visibility))
            throw new IllegalArgumentException("지원하지 않는 공개 범위입니다.");
        if (order < 0) throw new IllegalArgumentException("정렬 순서는 0 이상이어야 합니다.");
        sortOrder = order;
        updatedBy = actor;
        updatedAt = now;
    }

    private static Map<String, Object> immutablePayload(Map<String, Object> data) {
        Map<String, Object> copy = new LinkedHashMap<>();
        Objects.requireNonNull(data)
                .forEach(
                        (key, value) -> {
                            if (value != null) copy.put(Objects.requireNonNull(key), value);
                        });
        return Collections.unmodifiableMap(copy);
    }

    private static String text(String v, int max) {
        if (v == null || v.isBlank() || v.strip().length() > max)
            throw new IllegalArgumentException("필수 값을 확인해 주세요.");
        return v.strip();
    }

    public UUID id() {
        return id;
    }

    public UUID tripId() {
        return tripId;
    }

    public String type() {
        return type;
    }

    public UUID parentId() {
        return parentId;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public String status() {
        return status;
    }

    public String visibility() {
        return visibility;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public long version() {
        return version;
    }
}
