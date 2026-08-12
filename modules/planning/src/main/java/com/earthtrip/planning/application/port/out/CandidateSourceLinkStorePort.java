package com.earthtrip.planning.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateSourceLinkStorePort {

    Optional<LinkRecord> find(UUID candidateId, UUID sourceId);

    List<LinkRecord> findByCandidateId(UUID candidateId);

    List<LinkRecord> findBySourceId(UUID sourceId);

    LinkRecord save(LinkRecord record);

    void delete(UUID candidateId, UUID sourceId);

    record LinkRecord(
            UUID tripId, UUID candidateId, UUID sourceId, UUID linkedBy, Instant linkedAt) {}
}
