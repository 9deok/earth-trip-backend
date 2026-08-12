package com.earthtrip.expense.adapter.out.persistence.category;

import com.earthtrip.expense.application.port.out.ExpenseCategoryStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense_categories")
class ExpenseCategoryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExpenseCategoryJpaEntity() {}

    ExpenseCategoryJpaEntity(ExpenseCategoryStorePort.CategoryRecord record) {
        id = record.id().toString();
        apply(record);
    }

    void apply(ExpenseCategoryStorePort.CategoryRecord record) {
        tripId = record.tripId().toString();
        code = record.code();
        name = record.name();
        color = record.color();
        sortOrder = record.sortOrder();
        createdBy = record.createdBy().toString();
        updatedBy = record.updatedBy().toString();
        createdAt = record.createdAt();
        updatedAt = record.updatedAt();
        deletedAt = record.deletedAt();
    }

    ExpenseCategoryStorePort.CategoryRecord toRecord() {
        return new ExpenseCategoryStorePort.CategoryRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                code,
                name,
                color,
                sortOrder,
                UUID.fromString(createdBy),
                UUID.fromString(updatedBy),
                createdAt,
                updatedAt,
                deletedAt,
                version);
    }
}
