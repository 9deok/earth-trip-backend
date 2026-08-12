package com.earthtrip.planning.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface CandidateSourceLinkUseCase {

    LinkResult link(UUID tripId, UUID candidateId, UUID sourceId, UUID actorUserId);

    void unlink(UUID tripId, UUID candidateId, UUID sourceId, UUID actorUserId);

    record LinkResult(
            UUID tripId, UUID candidateId, UUID sourceId, UUID linkedBy, Instant linkedAt) {}
}
