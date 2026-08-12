package com.earthtrip.expense.adapter.out.persistence.review;

import com.earthtrip.expense.application.port.out.ExpenseReviewStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense_review_days")
@IdClass(ExpenseReviewDayId.class)
class ExpenseReviewDayJpaEntity {

    @Id
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Id
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(name = "completed_by", nullable = false, length = 36)
    private String completedBy;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExpenseReviewDayJpaEntity() {}

    ExpenseReviewDayJpaEntity(ExpenseReviewStorePort.ReviewRecord record) {
        tripId = record.tripId().toString();
        localDate = record.localDate();
        apply(record);
    }

    void apply(ExpenseReviewStorePort.ReviewRecord record) {
        completedBy = record.completedBy().toString();
        note = record.note();
        completedAt = record.completedAt();
    }

    ExpenseReviewStorePort.ReviewRecord toRecord() {
        return new ExpenseReviewStorePort.ReviewRecord(
                UUID.fromString(tripId),
                localDate,
                UUID.fromString(completedBy),
                note,
                completedAt,
                version);
    }
}
