package com.earthtrip.trip.adapter.out.persistence.destination;

import com.earthtrip.trip.domain.DestinationCandidate;
import com.earthtrip.trip.domain.TripId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "destination_candidates")
class DestinationCandidateJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DestinationCandidateJpaEntity() {}

    DestinationCandidateJpaEntity(DestinationCandidate c) {
        id = c.id().toString();
        apply(c);
    }

    void apply(DestinationCandidate c) {
        tripId = c.tripId().toString();
        name = c.name();
        countryCode = c.countryCode();
        placeId = c.placeId();
        latitude = c.latitude();
        longitude = c.longitude();
        note = c.note();
        status = c.status().name();
        createdBy = c.createdBy().toString();
        createdAt = c.createdAt();
        updatedAt = c.updatedAt();
    }

    DestinationCandidate toDomain() {
        return DestinationCandidate.restore(
                UUID.fromString(id),
                TripId.from(tripId),
                name,
                countryCode,
                placeId,
                latitude,
                longitude,
                note,
                DestinationCandidate.Status.valueOf(status),
                UUID.fromString(createdBy),
                createdAt,
                updatedAt,
                version);
    }
}
