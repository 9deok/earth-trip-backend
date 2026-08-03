package com.earthtrip.trip.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class DateCandidate {
    public enum Status { PROPOSED, SHORTLISTED, SELECTED, REJECTED }
    private final UUID id; private final TripId tripId; private LocalDate startDate; private LocalDate endDate;
    private String note; private Status status; private final UUID createdBy; private final Instant createdAt;
    private Instant updatedAt; private final long version;
    private DateCandidate(
        UUID id, TripId tripId, LocalDate startDate, LocalDate endDate, String note, Status status,
        UUID createdBy, Instant createdAt, Instant updatedAt, long version
    ) {
        this.id = Objects.requireNonNull(id); this.tripId = Objects.requireNonNull(tripId);
        apply(startDate, endDate, note, status, updatedAt); this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt); this.version = version;
    }
    public static DateCandidate create(
        UUID id, TripId tripId, LocalDate start, LocalDate end, String note, UUID actor, Instant now
    ) { return new DateCandidate(id, tripId, start, end, note, Status.PROPOSED, actor, now, now, 0); }
    public static DateCandidate restore(
        UUID id, TripId tripId, LocalDate start, LocalDate end, String note, Status status,
        UUID actor, Instant created, Instant updated, long version
    ) { return new DateCandidate(id, tripId, start, end, note, status, actor, created, updated, version); }
    public void update(LocalDate start, LocalDate end, String note, String status, Instant now) {
        apply(
            start == null ? startDate : start, end == null ? endDate : end,
            note == null ? this.note : note,
            status == null ? this.status : Status.valueOf(status.strip().toUpperCase(Locale.ROOT)), now
        );
    }
    private void apply(LocalDate start, LocalDate end, String note, Status status, Instant now) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("날짜 후보의 시작일과 종료일을 확인해 주세요.");
        }
        this.startDate = start; this.endDate = end;
        this.note = note == null || note.isBlank() ? null : note.strip();
        if (this.note != null && this.note.length() > 5000) throw new IllegalArgumentException("메모가 너무 깁니다.");
        this.status = Objects.requireNonNull(status); this.updatedAt = Objects.requireNonNull(now);
    }
    public UUID id(){return id;} public TripId tripId(){return tripId;}
    public LocalDate startDate(){return startDate;} public LocalDate endDate(){return endDate;}
    public String note(){return note;} public Status status(){return status;}
    public UUID createdBy(){return createdBy;} public Instant createdAt(){return createdAt;}
    public Instant updatedAt(){return updatedAt;} public long version(){return version;}
}
