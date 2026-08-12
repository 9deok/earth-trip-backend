package com.earthtrip.trip.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Trip {

    public enum Status {
        DRAFT,
        PLANNING,
        TRAVELING,
        COMPLETED,
        ARCHIVED,
        DELETION_PENDING
    }

    public enum PlanningMode {
        EXACT,
        FLEXIBLE,
        HYBRID
    }

    public enum Pace {
        RELAXED,
        BALANCED,
        PACKED
    }

    public enum DateMode {
        EXACT,
        CANDIDATES,
        UNDECIDED
    }

    public enum TravelMode {
        ROUND_TRIP,
        ONE_WAY,
        OPEN_JAW
    }

    private final TripId id;
    private UUID ownerUserId;
    private TripTitle title;
    private Status status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timeZone;
    private String defaultCurrency;
    private PlanningMode planningMode;
    private Pace pace;
    private int companionCount;
    private List<String> companionNames;
    private DateMode dateMode;
    private TravelMode travelMode;
    private String departurePoint;
    private String returnPoint;
    private int firstDayStartMinutes;
    private int lastDayEndMinutes;
    private int overnightTravelNights;
    private boolean reduceStairs;
    private boolean frequentBreaks;
    private int walkingLimitMinutes;
    private String dietaryNotes;
    private Instant deletedAt;
    private Instant scheduledDeletionAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private final long version;

    private Trip(
            TripId id,
            UUID ownerUserId,
            TripTitle title,
            Status status,
            LocalDate startDate,
            LocalDate endDate,
            String timeZone,
            String defaultCurrency,
            PlanningMode planningMode,
            Pace pace,
            int companionCount,
            List<String> companionNames,
            DateMode dateMode,
            TravelMode travelMode,
            String departurePoint,
            String returnPoint,
            int firstDayStartMinutes,
            int lastDayEndMinutes,
            int overnightTravelNights,
            boolean reduceStairs,
            boolean frequentBreaks,
            int walkingLimitMinutes,
            String dietaryNotes,
            Instant deletedAt,
            Instant scheduledDeletionAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.ownerUserId = ownerUserId;
        this.title = Objects.requireNonNull(title);
        this.status = Objects.requireNonNull(status);
        TripPolicy.dates(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeZone = TripPolicy.timeZone(timeZone);
        this.defaultCurrency = TripPolicy.currency(defaultCurrency);
        this.planningMode = Objects.requireNonNull(planningMode);
        this.pace = Objects.requireNonNull(pace);
        applyPreferences(
                companionCount,
                companionNames,
                dateMode,
                travelMode,
                departurePoint,
                returnPoint,
                firstDayStartMinutes,
                lastDayEndMinutes,
                overnightTravelNights,
                reduceStairs,
                frequentBreaks,
                walkingLimitMinutes,
                dietaryNotes);
        this.deletedAt = deletedAt;
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
    }

    public static Trip create(
            TripId id,
            UUID ownerUserId,
            TripTitle title,
            String timeZone,
            String defaultCurrency,
            Instant now) {
        return draft(
                id, Objects.requireNonNull(ownerUserId), title, timeZone, defaultCurrency, now);
    }

    private static Trip draft(
            TripId id,
            UUID ownerUserId,
            TripTitle title,
            String timeZone,
            String defaultCurrency,
            Instant now) {
        return new Trip(
                id,
                ownerUserId,
                title,
                Status.DRAFT,
                null,
                null,
                timeZone,
                defaultCurrency,
                PlanningMode.EXACT,
                Pace.BALANCED,
                1,
                List.of("나"),
                DateMode.EXACT,
                TravelMode.ROUND_TRIP,
                "",
                "",
                600,
                1080,
                0,
                false,
                true,
                90,
                "",
                null,
                null,
                now,
                now,
                0);
    }

    public static Trip restore(
            TripId id,
            UUID ownerUserId,
            TripTitle title,
            Status status,
            LocalDate startDate,
            LocalDate endDate,
            String timeZone,
            String defaultCurrency,
            PlanningMode planningMode,
            Pace pace,
            int companionCount,
            List<String> companionNames,
            DateMode dateMode,
            TravelMode travelMode,
            String departurePoint,
            String returnPoint,
            int firstDayStartMinutes,
            int lastDayEndMinutes,
            int overnightTravelNights,
            boolean reduceStairs,
            boolean frequentBreaks,
            int walkingLimitMinutes,
            String dietaryNotes,
            Instant deletedAt,
            Instant scheduledDeletionAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new Trip(
                id,
                ownerUserId,
                title,
                status,
                startDate,
                endDate,
                timeZone,
                defaultCurrency,
                planningMode,
                pace,
                companionCount,
                companionNames,
                dateMode,
                travelMode,
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
                version);
    }

    public void update(
            String newTitle,
            String newStatus,
            LocalDate newStartDate,
            LocalDate newEndDate,
            String newTimeZone,
            String newCurrency,
            String newPlanningMode,
            String newPace,
            Integer newCompanionCount,
            List<String> newCompanionNames,
            String newDateMode,
            String newTravelMode,
            String newDeparturePoint,
            String newReturnPoint,
            Integer newFirstDayStartMinutes,
            Integer newLastDayEndMinutes,
            Integer newOvernightTravelNights,
            Boolean newReduceStairs,
            Boolean newFrequentBreaks,
            Integer newWalkingLimitMinutes,
            String newDietaryNotes,
            Instant now) {
        TripTitle candidateTitle = newTitle == null ? title : new TripTitle(newTitle);
        Status candidateStatus = TripPolicy.updatedStatus(status, newStatus);
        LocalDate candidateStart = newStartDate == null ? startDate : newStartDate;
        LocalDate candidateEnd = newEndDate == null ? endDate : newEndDate;
        TripPolicy.dates(candidateStart, candidateEnd);
        this.title = candidateTitle;
        this.status = candidateStatus;
        this.startDate = candidateStart;
        this.endDate = candidateEnd;
        this.timeZone = newTimeZone == null ? timeZone : TripPolicy.timeZone(newTimeZone);
        this.defaultCurrency =
                newCurrency == null ? defaultCurrency : TripPolicy.currency(newCurrency);
        this.planningMode =
                newPlanningMode == null
                        ? planningMode
                        : PlanningMode.valueOf(newPlanningMode.toUpperCase(Locale.ROOT));
        this.pace = newPace == null ? pace : Pace.valueOf(newPace.toUpperCase(Locale.ROOT));
        applyPreferences(
                newCompanionCount == null ? companionCount : newCompanionCount,
                newCompanionNames == null ? companionNames : newCompanionNames,
                newDateMode == null
                        ? dateMode
                        : DateMode.valueOf(newDateMode.toUpperCase(Locale.ROOT)),
                newTravelMode == null
                        ? travelMode
                        : TravelMode.valueOf(newTravelMode.toUpperCase(Locale.ROOT)),
                newDeparturePoint == null ? departurePoint : newDeparturePoint,
                newReturnPoint == null ? returnPoint : newReturnPoint,
                newFirstDayStartMinutes == null ? firstDayStartMinutes : newFirstDayStartMinutes,
                newLastDayEndMinutes == null ? lastDayEndMinutes : newLastDayEndMinutes,
                newOvernightTravelNights == null ? overnightTravelNights : newOvernightTravelNights,
                newReduceStairs == null ? reduceStairs : newReduceStairs,
                newFrequentBreaks == null ? frequentBreaks : newFrequentBreaks,
                newWalkingLimitMinutes == null ? walkingLimitMinutes : newWalkingLimitMinutes,
                newDietaryNotes == null ? dietaryNotes : newDietaryNotes);
        this.updatedAt = Objects.requireNonNull(now);
    }

    private void applyPreferences(
            int companionCount,
            List<String> companionNames,
            DateMode dateMode,
            TravelMode travelMode,
            String departurePoint,
            String returnPoint,
            int firstDayStartMinutes,
            int lastDayEndMinutes,
            int overnightTravelNights,
            boolean reduceStairs,
            boolean frequentBreaks,
            int walkingLimitMinutes,
            String dietaryNotes) {
        TripPolicy.Preferences normalized =
                TripPolicy.preferences(
                        companionCount,
                        companionNames,
                        dateMode,
                        travelMode,
                        departurePoint,
                        returnPoint,
                        firstDayStartMinutes,
                        lastDayEndMinutes,
                        overnightTravelNights,
                        reduceStairs,
                        frequentBreaks,
                        walkingLimitMinutes,
                        dietaryNotes);
        this.companionCount = normalized.companionCount();
        this.companionNames = normalized.companionNames();
        this.dateMode = normalized.dateMode();
        this.travelMode = normalized.travelMode();
        this.departurePoint = normalized.departurePoint();
        this.returnPoint = normalized.returnPoint();
        this.firstDayStartMinutes = normalized.firstDayStartMinutes();
        this.lastDayEndMinutes = normalized.lastDayEndMinutes();
        this.overnightTravelNights = normalized.overnightTravelNights();
        this.reduceStairs = normalized.reduceStairs();
        this.frequentBreaks = normalized.frequentBreaks();
        this.walkingLimitMinutes = normalized.walkingLimitMinutes();
        this.dietaryNotes = normalized.dietaryNotes();
    }

    public void rename(TripTitle newTitle, Instant now) {
        title = Objects.requireNonNull(newTitle);
        updatedAt = Objects.requireNonNull(now);
    }

    public void requestDeletion(Instant now, Instant scheduledAt) {
        if (status == Status.DELETION_PENDING) return;
        status = Status.DELETION_PENDING;
        deletedAt = Objects.requireNonNull(now);
        scheduledDeletionAt = Objects.requireNonNull(scheduledAt);
        updatedAt = now;
    }

    public void restoreDeletion(Instant now) {
        if (status != Status.DELETION_PENDING) {
            throw new IllegalStateException("삭제 대기 중인 여행만 복구할 수 있습니다.");
        }
        status = startDate == null ? Status.DRAFT : Status.PLANNING;
        deletedAt = null;
        scheduledDeletionAt = null;
        updatedAt = Objects.requireNonNull(now);
    }

    public boolean isOwnedBy(UUID userId) {
        return Objects.equals(ownerUserId, userId);
    }

    public void transferOwnership(UUID newOwnerUserId, Instant now) {
        if (status == Status.DELETION_PENDING) {
            throw new IllegalStateException("삭제 대기 중인 여행의 소유권은 이전할 수 없습니다.");
        }
        ownerUserId = Objects.requireNonNull(newOwnerUserId);
        updatedAt = Objects.requireNonNull(now);
    }

    public TripId id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public TripTitle title() {
        return title;
    }

    public Status status() {
        return status;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public String timeZone() {
        return timeZone;
    }

    public String defaultCurrency() {
        return defaultCurrency;
    }

    public PlanningMode planningMode() {
        return planningMode;
    }

    public Pace pace() {
        return pace;
    }

    public int companionCount() {
        return companionCount;
    }

    public List<String> companionNames() {
        return companionNames;
    }

    public DateMode dateMode() {
        return dateMode;
    }

    public TravelMode travelMode() {
        return travelMode;
    }

    public String departurePoint() {
        return departurePoint;
    }

    public String returnPoint() {
        return returnPoint;
    }

    public int firstDayStartMinutes() {
        return firstDayStartMinutes;
    }

    public int lastDayEndMinutes() {
        return lastDayEndMinutes;
    }

    public int overnightTravelNights() {
        return overnightTravelNights;
    }

    public boolean reduceStairs() {
        return reduceStairs;
    }

    public boolean frequentBreaks() {
        return frequentBreaks;
    }

    public int walkingLimitMinutes() {
        return walkingLimitMinutes;
    }

    public String dietaryNotes() {
        return dietaryNotes;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public Instant scheduledDeletionAt() {
        return scheduledDeletionAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
