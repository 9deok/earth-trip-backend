package com.earthtrip.planning.adapter.out.persistence.link;

import com.earthtrip.planning.application.port.out.CandidateSourceLinkStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_source_links")
class CandidateSourceLinkJpaEntity {

    @EmbeddedId
    private CandidateSourceLinkId id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "linked_by", nullable = false, length = 36)
    private String linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected CandidateSourceLinkJpaEntity() { }

    CandidateSourceLinkJpaEntity(CandidateSourceLinkStorePort.LinkRecord record) {
        id = new CandidateSourceLinkId(
            record.candidateId().toString(), record.sourceId().toString()
        );
        tripId = record.tripId().toString();
        linkedBy = record.linkedBy().toString();
        linkedAt = record.linkedAt();
    }

    CandidateSourceLinkStorePort.LinkRecord toRecord() {
        return new CandidateSourceLinkStorePort.LinkRecord(
            UUID.fromString(tripId), UUID.fromString(id.candidateId()),
            UUID.fromString(id.sourceId()), UUID.fromString(linkedBy), linkedAt
        );
    }
}
