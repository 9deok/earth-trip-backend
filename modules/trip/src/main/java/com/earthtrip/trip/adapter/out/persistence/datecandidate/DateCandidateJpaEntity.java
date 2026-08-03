package com.earthtrip.trip.adapter.out.persistence.datecandidate;

import com.earthtrip.trip.domain.DateCandidate;
import com.earthtrip.trip.domain.TripId;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="date_candidates")
class DateCandidateJpaEntity {
    @Id @Column(name="id",nullable=false,length=36) private String id;
    @Column(name="trip_id",nullable=false,length=36) private String tripId;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(name="note",columnDefinition="TEXT") private String note;
    @Column(name="status",nullable=false,length=30) private String status;
    @Column(name="created_by",nullable=false,length=36) private String createdBy;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(name="version",nullable=false) private long version;
    protected DateCandidateJpaEntity() { }
    DateCandidateJpaEntity(DateCandidate c){id=c.id().toString();apply(c);}
    void apply(DateCandidate c){tripId=c.tripId().toString();startDate=c.startDate();endDate=c.endDate();
        note=c.note();status=c.status().name();createdBy=c.createdBy().toString();createdAt=c.createdAt();updatedAt=c.updatedAt();}
    DateCandidate toDomain(){return DateCandidate.restore(UUID.fromString(id), TripId.from(tripId),startDate,endDate,note,
        DateCandidate.Status.valueOf(status),UUID.fromString(createdBy),createdAt,updatedAt,version);}
}
