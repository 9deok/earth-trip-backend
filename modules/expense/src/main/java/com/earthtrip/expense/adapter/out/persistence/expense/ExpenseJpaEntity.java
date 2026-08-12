package com.earthtrip.expense.adapter.out.persistence.expense;

import com.earthtrip.expense.domain.Expense;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "expenses")
class ExpenseJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "category_code", nullable = false, length = 80)
    private String category;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "payer_contributions", nullable = false, columnDefinition = "JSON")
    private String payers;

    @Column(name = "participant_shares", nullable = false, columnDefinition = "JSON")
    private String shares;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

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

    protected ExpenseJpaEntity() {}

    ExpenseJpaEntity(Expense e, String p, String s) {
        id = e.id().toString();
        apply(e, p, s);
    }

    void apply(Expense e, String p, String s) {
        tripId = e.tripId().toString();
        title = e.title();
        category = e.categoryCode();
        amountMinor = e.amountMinor();
        currency = e.currency();
        occurredAt = e.occurredAt();
        payers = p;
        shares = s;
        visibility = e.visibility();
        status = e.status();
        note = e.note();
        createdBy = e.createdBy().toString();
        updatedBy = e.updatedBy().toString();
        createdAt = e.createdAt();
        updatedAt = e.updatedAt();
        deletedAt = e.deletedAt();
    }

    String payers() {
        return payers;
    }

    String shares() {
        return shares;
    }

    Expense toDomain(Map<UUID, Long> p, Map<UUID, Long> s) {
        return Expense.restore(
                UUID.fromString(id),
                UUID.fromString(tripId),
                title,
                category,
                amountMinor,
                currency,
                occurredAt,
                p,
                s,
                visibility,
                status,
                note,
                UUID.fromString(createdBy),
                UUID.fromString(updatedBy),
                createdAt,
                updatedAt,
                deletedAt,
                version);
    }
}
