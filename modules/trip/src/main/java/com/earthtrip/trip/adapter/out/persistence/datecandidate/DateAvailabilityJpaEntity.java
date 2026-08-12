package com.earthtrip.trip.adapter.out.persistence.datecandidate;

import com.earthtrip.trip.application.port.out.DateCandidateStorePort;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "date_candidate_availability")
@IdClass(DateAvailabilityId.class)
class DateAvailabilityJpaEntity {
    @Id
    @Column(name = "candidate_id", nullable = false, length = 36)
    private String candidateId;

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "availability", nullable = false, length = 20)
    private String availability;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DateAvailabilityJpaEntity() {}

    DateAvailabilityJpaEntity(String c, String u, String a, String n, Instant now) {
        candidateId = c;
        userId = u;
        apply(a, n, now);
    }

    void apply(String a, String n, Instant now) {
        availability = a;
        note = n;
        updatedAt = now;
    }

    UUID candidateId() {
        return UUID.fromString(candidateId);
    }

    DateCandidateStorePort.AvailabilityRecord toRecord() {
        return new DateCandidateStorePort.AvailabilityRecord(
                UUID.fromString(userId), availability, note, updatedAt);
    }
}
