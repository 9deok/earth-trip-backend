package com.earthtrip.trip.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Trip {

    public enum Status { DRAFT, PLANNING, TRAVELING, COMPLETED, ARCHIVED, DELETION_PENDING }

    public enum PlanningMode { EXACT, FLEXIBLE, HYBRID }

    public enum Pace { RELAXED, BALANCED, PACKED }

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
        Instant deletedAt,
        Instant scheduledDeletionAt,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerUserId = ownerUserId;
        this.title = Objects.requireNonNull(title);
        this.status = Objects.requireNonNull(status);
        validateDates(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeZone = validTimeZone(timeZone);
        this.defaultCurrency = validCurrency(defaultCurrency);
        this.planningMode = Objects.requireNonNull(planningMode);
        this.pace = Objects.requireNonNull(pace);
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
        Instant now
    ) {
        return new Trip(
            id, Objects.requireNonNull(ownerUserId), title, Status.DRAFT, null, null,
            timeZone, defaultCurrency, PlanningMode.EXACT, Pace.BALANCED,
            null, null, now, now, 0
        );
    }

    /** 기존 도메인 단위 테스트를 위한 최소 생성 팩토리입니다. */
    public static Trip create(TripId id, TripTitle title, Instant now) {
        return new Trip(
            id, null, title, Status.DRAFT, null, null, "Asia/Seoul", "KRW",
            PlanningMode.EXACT, Pace.BALANCED, null, null, now, now, 0
        );
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
        Instant deletedAt,
        Instant scheduledDeletionAt,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        return new Trip(
            id, ownerUserId, title, status, startDate, endDate, timeZone, defaultCurrency,
            planningMode, pace, deletedAt, scheduledDeletionAt, createdAt, updatedAt, version
        );
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
        Instant now
    ) {
        TripTitle candidateTitle = newTitle == null ? title : new TripTitle(newTitle);
        Status candidateStatus = newStatus == null ? status : Status.valueOf(newStatus.toUpperCase(Locale.ROOT));
        LocalDate candidateStart = newStartDate == null ? startDate : newStartDate;
        LocalDate candidateEnd = newEndDate == null ? endDate : newEndDate;
        validateDates(candidateStart, candidateEnd);
        this.title = candidateTitle;
        this.status = candidateStatus;
        this.startDate = candidateStart;
        this.endDate = candidateEnd;
        this.timeZone = newTimeZone == null ? timeZone : validTimeZone(newTimeZone);
        this.defaultCurrency = newCurrency == null ? defaultCurrency : validCurrency(newCurrency);
        this.planningMode = newPlanningMode == null
            ? planningMode
            : PlanningMode.valueOf(newPlanningMode.toUpperCase(Locale.ROOT));
        this.pace = newPace == null ? pace : Pace.valueOf(newPace.toUpperCase(Locale.ROOT));
        this.deletedAt = null;
        this.scheduledDeletionAt = null;
        this.updatedAt = Objects.requireNonNull(now);
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

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("출발일과 종료일은 함께 입력해야 합니다.");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("여행 종료일은 출발일보다 빠를 수 없습니다.");
        }
        if (startDate != null && java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw new IllegalArgumentException("한 여행은 최대 366일까지 설정할 수 있습니다.");
        }
    }

    private static String validTimeZone(String value) {
        return ZoneId.of(Objects.requireNonNull(value).strip()).getId();
    }

    private static String validCurrency(String value) {
        return Currency.getInstance(value.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
    }

    public TripId id() { return id; }
    public UUID ownerUserId() { return ownerUserId; }
    public TripTitle title() { return title; }
    public Status status() { return status; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public String timeZone() { return timeZone; }
    public String defaultCurrency() { return defaultCurrency; }
    public PlanningMode planningMode() { return planningMode; }
    public Pace pace() { return pace; }
    public Instant deletedAt() { return deletedAt; }
    public Instant scheduledDeletionAt() { return scheduledDeletionAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
