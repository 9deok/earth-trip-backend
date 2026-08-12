package com.earthtrip.trip.adapter.out.persistence.segment;

import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripSegment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trip_segments")
class TripSegmentJpaEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "segment_type", nullable = false, length = 30)
    private String type;

    @Column(name = "city_name", length = 160)
    private String cityName;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "accommodation_name", length = 200)
    private String accommodationName;

    @Column(name = "accommodation_place_id", length = 255)
    private String accommodationPlaceId;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "transport_mode", length = 40)
    private String transportMode;

    @Column(name = "departure_at")
    private Instant departureAt;

    @Column(name = "arrival_at")
    private Instant arrivalAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TripSegmentJpaEntity() {}

    private TripSegmentJpaEntity(TripSegment segment) {
        id = segment.id().toString();
        apply(segment);
    }

    static TripSegmentJpaEntity from(TripSegment segment) {
        return new TripSegmentJpaEntity(segment);
    }

    void apply(TripSegment segment) {
        tripId = segment.tripId().toString();
        type = segment.type().name();
        cityName = segment.cityName();
        countryCode = segment.countryCode();
        placeId = segment.placeId();
        latitude = segment.latitude();
        longitude = segment.longitude();
        startDate = segment.startDate();
        endDate = segment.endDate();
        accommodationName = segment.accommodationName();
        accommodationPlaceId = segment.accommodationPlaceId();
        checkInAt = segment.checkInAt();
        checkOutAt = segment.checkOutAt();
        transportMode = segment.transportMode();
        departureAt = segment.departureAt();
        arrivalAt = segment.arrivalAt();
        sortOrder = segment.sortOrder();
        createdBy = segment.createdBy().toString();
        updatedBy = segment.updatedBy().toString();
        createdAt = segment.createdAt();
        updatedAt = segment.updatedAt();
    }

    TripSegment toDomain() {
        return TripSegment.restore(
                UUID.fromString(id),
                TripId.from(tripId),
                TripSegment.Type.valueOf(type),
                cityName,
                countryCode,
                placeId,
                latitude,
                longitude,
                startDate,
                endDate,
                accommodationName,
                accommodationPlaceId,
                checkInAt,
                checkOutAt,
                transportMode,
                departureAt,
                arrivalAt,
                sortOrder,
                UUID.fromString(createdBy),
                UUID.fromString(updatedBy),
                createdAt,
                updatedAt,
                version);
    }
}
