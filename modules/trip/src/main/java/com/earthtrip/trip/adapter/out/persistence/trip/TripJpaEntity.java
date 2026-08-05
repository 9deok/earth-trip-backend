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

    @Column(name = "companion_count", nullable = false)
    private int companionCount;

    @Column(name = "companion_names_json", nullable = false, columnDefinition = "TEXT")
    private String companionNamesJson;

    @Column(name = "date_mode", nullable = false, length = 30)
    private String dateMode;

    @Column(name = "travel_mode", nullable = false, length = 30)
    private String travelMode;

    @Column(name = "departure_point", nullable = false, length = 200)
    private String departurePoint;

    @Column(name = "return_point", nullable = false, length = 200)
    private String returnPoint;

    @Column(name = "first_day_start_minutes", nullable = false)
    private int firstDayStartMinutes;

    @Column(name = "last_day_end_minutes", nullable = false)
    private int lastDayEndMinutes;

    @Column(name = "overnight_travel_nights", nullable = false)
    private int overnightTravelNights;

    @Column(name = "reduce_stairs", nullable = false)
    private boolean reduceStairs;

    @Column(name = "frequent_breaks", nullable = false)
    private boolean frequentBreaks;

    @Column(name = "walking_limit_minutes", nullable = false)
    private int walkingLimitMinutes;

    @Column(name = "dietary_notes", nullable = false, length = 2000)
    private String dietaryNotes;

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

    private TripJpaEntity(Trip trip, String companionNamesJson) {
        this.id = trip.id().toString();
        apply(trip, companionNamesJson);
    }

    static TripJpaEntity from(Trip trip, String companionNamesJson) {
        return new TripJpaEntity(trip, companionNamesJson);
    }

    void apply(Trip trip, String companionNamesJson) {
        this.ownerUserId = trip.ownerUserId() == null ? null : trip.ownerUserId().toString();
        this.title = trip.title().value();
        this.status = trip.status().name();
        this.startDate = trip.startDate();
        this.endDate = trip.endDate();
        this.timeZone = trip.timeZone();
        this.defaultCurrency = trip.defaultCurrency();
        this.planningMode = trip.planningMode().name();
        this.pace = trip.pace().name();
        this.companionCount = trip.companionCount();
        this.companionNamesJson = companionNamesJson;
        this.dateMode = trip.dateMode().name();
        this.travelMode = trip.travelMode().name();
        this.departurePoint = trip.departurePoint();
        this.returnPoint = trip.returnPoint();
        this.firstDayStartMinutes = trip.firstDayStartMinutes();
        this.lastDayEndMinutes = trip.lastDayEndMinutes();
        this.overnightTravelNights = trip.overnightTravelNights();
        this.reduceStairs = trip.reduceStairs();
        this.frequentBreaks = trip.frequentBreaks();
        this.walkingLimitMinutes = trip.walkingLimitMinutes();
        this.dietaryNotes = trip.dietaryNotes();
        this.deletedAt = trip.deletedAt();
        this.scheduledDeletionAt = trip.scheduledDeletionAt();
        this.createdAt = trip.createdAt();
        this.updatedAt = trip.updatedAt();
    }

    Trip toDomain(java.util.List<String> companionNames) {
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
            companionCount,
            companionNames,
            Trip.DateMode.valueOf(dateMode),
            Trip.TravelMode.valueOf(travelMode),
            departurePoint,
            returnPoint,
            firstDayStartMinutes,
            lastDayEndMinutes,
            overnightTravelNights,
            reduceStairs,
            frequentBreaks,
            walkingLimitMinutes,
            dietaryNotes,
            deletedAt,
            scheduledDeletionAt,
            createdAt,
            updatedAt,
            version
        );
    }

    String companionNamesJson() { return companionNamesJson; }
}
