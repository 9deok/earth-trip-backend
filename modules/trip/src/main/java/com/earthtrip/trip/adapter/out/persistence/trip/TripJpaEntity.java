package com.earthtrip.trip.adapter.out.persistence.trip;

import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trips")
class TripJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "owner_user_id", length = 36)
    private String ownerUserId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "planning_mode", nullable = false, length = 30)
    private String planningMode;

    @Column(name = "pace", nullable = false, length = 30)
    private String pace;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "scheduled_deletion_at")
    private Instant scheduledDeletionAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TripJpaEntity() { }

    private TripJpaEntity(Trip trip) {
        this.id = trip.id().toString();
        apply(trip);
    }

    static TripJpaEntity from(Trip trip) {
        return new TripJpaEntity(trip);
    }

    void apply(Trip trip) {
        this.ownerUserId = trip.ownerUserId() == null ? null : trip.ownerUserId().toString();
        this.title = trip.title().value();
        this.status = trip.status().name();
        this.startDate = trip.startDate();
        this.endDate = trip.endDate();
        this.timeZone = trip.timeZone();
        this.defaultCurrency = trip.defaultCurrency();
        this.planningMode = trip.planningMode().name();
        this.pace = trip.pace().name();
        this.deletedAt = trip.deletedAt();
        this.scheduledDeletionAt = trip.scheduledDeletionAt();
        this.createdAt = trip.createdAt();
        this.updatedAt = trip.updatedAt();
    }

    Trip toDomain() {
        return Trip.restore(
            TripId.from(id),
            ownerUserId == null ? null : UUID.fromString(ownerUserId),
            new TripTitle(title),
            Trip.Status.valueOf(status),
            startDate,
            endDate,
            timeZone,
            defaultCurrency,
            Trip.PlanningMode.valueOf(planningMode),
            Trip.Pace.valueOf(pace),
            deletedAt,
            scheduledDeletionAt,
            createdAt,
            updatedAt,
            version
        );
    }
}
