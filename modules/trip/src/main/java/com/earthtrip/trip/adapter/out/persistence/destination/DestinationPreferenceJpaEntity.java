package com.earthtrip.trip.adapter.out.persistence.destination;

import com.earthtrip.trip.application.port.out.DestinationCandidateStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "destination_candidate_preferences") @IdClass(DestinationPreferenceId.class)
class DestinationPreferenceJpaEntity {
    @Id @Column(name = "candidate_id", nullable = false, length = 36) private String candidateId;
    @Id @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(name = "preference", nullable = false, length = 20) private String preference;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected DestinationPreferenceJpaEntity() { }
    DestinationPreferenceJpaEntity(String candidateId, String userId, String preference, Instant now) {
        this.candidateId = candidateId; this.userId = userId; apply(preference, now);
    }
    void apply(String value, Instant now) { preference = value; updatedAt = now; }
    DestinationCandidateStorePort.PreferenceRecord toRecord() {
        return new DestinationCandidateStorePort.PreferenceRecord(
            UUID.fromString(userId), preference, updatedAt
        );
    }
}
