package com.earthtrip.planning.adapter.out.persistence.link;

import com.earthtrip.planning.application.port.out.CandidateSourceLinkStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CandidateSourceLinkPersistenceAdapter implements CandidateSourceLinkStorePort {

    private final CandidateSourceLinkJpaRepository repository;

    CandidateSourceLinkPersistenceAdapter(CandidateSourceLinkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LinkRecord> find(UUID candidateId, UUID sourceId) {
        return repository.findById(new CandidateSourceLinkId(
                candidateId.toString(), sourceId.toString()
            ))
            .map(CandidateSourceLinkJpaEntity::toRecord);
    }

    @Override
    public List<LinkRecord> findByCandidateId(UUID candidateId) {
        return repository.findAllById_CandidateId(candidateId.toString()).stream()
            .map(CandidateSourceLinkJpaEntity::toRecord)
            .toList();
    }

    @Override
    public List<LinkRecord> findBySourceId(UUID sourceId) {
        return repository.findAllById_SourceId(sourceId.toString()).stream()
            .map(CandidateSourceLinkJpaEntity::toRecord)
            .toList();
    }

    @Override
    public LinkRecord save(LinkRecord record) {
        return repository.save(new CandidateSourceLinkJpaEntity(record)).toRecord();
    }

    @Override
    public void delete(UUID candidateId, UUID sourceId) {
        repository.deleteById(new CandidateSourceLinkId(
            candidateId.toString(), sourceId.toString()
        ));
    }
}
