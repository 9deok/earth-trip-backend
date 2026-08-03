package com.earthtrip.planning.application.service.link;

import com.earthtrip.planning.application.port.in.CandidateSourceLinkUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.out.CandidateSourceLinkStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CandidateSourceLinkService implements CandidateSourceLinkUseCase {

    private final TripAccess access;
    private final PlanningResourceUseCase resources;
    private final CandidateSourceLinkStorePort links;
    private final Clock clock;

    CandidateSourceLinkService(
        TripAccess access,
        PlanningResourceUseCase resources,
        CandidateSourceLinkStorePort links,
        Clock clock
    ) {
        this.access = access;
        this.resources = resources;
        this.links = links;
        this.clock = clock;
    }

    @Override
    public LinkResult link(
        UUID tripId,
        UUID candidateId,
        UUID sourceId,
        UUID actorUserId
    ) {
        access.requireEditor(tripId, actorUserId);
        requireResources(tripId, candidateId, sourceId, actorUserId);
        CandidateSourceLinkStorePort.LinkRecord existing = links.find(
            candidateId, sourceId
        ).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)) {
                throw EarthTripException.conflict(
                    "CANDIDATE_SOURCE_LINK_CONFLICT",
                    "다른 여행의 자료 연결과 충돌했습니다."
                );
            }
            return result(existing);
        }
        return result(links.save(new CandidateSourceLinkStorePort.LinkRecord(
            tripId, candidateId, sourceId, actorUserId, clock.instant()
        )));
    }

    @Override
    public void unlink(
        UUID tripId,
        UUID candidateId,
        UUID sourceId,
        UUID actorUserId
    ) {
        access.requireEditor(tripId, actorUserId);
        requireResources(tripId, candidateId, sourceId, actorUserId);
        CandidateSourceLinkStorePort.LinkRecord link = links.find(candidateId, sourceId)
            .filter(candidate -> candidate.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "CANDIDATE_SOURCE_LINK_NOT_FOUND",
                "장소 후보와 원본 자료의 연결을 찾을 수 없습니다."
            ));
        links.delete(link.candidateId(), link.sourceId());
    }

    private void requireResources(
        UUID tripId,
        UUID candidateId,
        UUID sourceId,
        UUID actorUserId
    ) {
        resources.get(tripId, actorUserId, "PLACE_CANDIDATE", candidateId);
        resources.get(tripId, actorUserId, "RESEARCH_SOURCE", sourceId);
    }

    private static LinkResult result(CandidateSourceLinkStorePort.LinkRecord record) {
        return new LinkResult(
            record.tripId(), record.candidateId(), record.sourceId(),
            record.linkedBy(), record.linkedAt()
        );
    }
}
