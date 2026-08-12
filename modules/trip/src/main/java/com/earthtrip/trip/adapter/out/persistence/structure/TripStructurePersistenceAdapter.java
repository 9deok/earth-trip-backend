package com.earthtrip.trip.adapter.out.persistence.structure;

import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripStructurePersistenceAdapter implements TripStructureStorePort {

    private final StructureChangeSetJpaRepository changeSets;
    private final DiagnosticResolutionJpaRepository resolutions;

    TripStructurePersistenceAdapter(
            StructureChangeSetJpaRepository changeSets,
            DiagnosticResolutionJpaRepository resolutions) {
        this.changeSets = changeSets;
        this.resolutions = resolutions;
    }

    @Override
    public Optional<ChangeSetRecord> changeSet(UUID changeSetId) {
        return changeSets
                .findById(changeSetId.toString())
                .map(StructureChangeSetJpaEntity::toRecord);
    }

    @Override
    public ChangeSetRecord saveChangeSet(ChangeSetRecord record) {
        StructureChangeSetJpaEntity entity =
                changeSets
                        .findById(record.id().toString())
                        .map(
                                current -> {
                                    current.apply(record);
                                    return current;
                                })
                        .orElseGet(() -> new StructureChangeSetJpaEntity(record));
        return changeSets.saveAndFlush(entity).toRecord();
    }

    @Override
    public List<ResolutionRecord> resolutions(UUID tripId) {
        return resolutions.findAllByTripId(tripId.toString()).stream()
                .map(DiagnosticResolutionJpaEntity::toRecord)
                .toList();
    }

    @Override
    public Optional<ResolutionRecord> resolution(UUID diagnosticId) {
        return resolutions
                .findById(diagnosticId.toString())
                .map(DiagnosticResolutionJpaEntity::toRecord);
    }

    @Override
    public ResolutionRecord saveResolution(ResolutionRecord record) {
        DiagnosticResolutionJpaEntity entity =
                resolutions
                        .findById(record.diagnosticId().toString())
                        .map(
                                current -> {
                                    current.apply(record);
                                    return current;
                                })
                        .orElseGet(() -> new DiagnosticResolutionJpaEntity(record));
        return resolutions.save(entity).toRecord();
    }

    @Override
    public void deleteResolution(UUID diagnosticId) {
        resolutions.deleteById(diagnosticId.toString());
    }
}
