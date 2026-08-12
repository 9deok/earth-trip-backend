package com.earthtrip.planning.adapter.out.persistence.link;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CandidateSourceLinkJpaRepository
        extends JpaRepository<CandidateSourceLinkJpaEntity, CandidateSourceLinkId> {

    List<CandidateSourceLinkJpaEntity> findAllById_CandidateId(String candidateId);

    List<CandidateSourceLinkJpaEntity> findAllById_SourceId(String sourceId);
}
