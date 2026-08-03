package com.earthtrip.planning.adapter.out.persistence.change;

import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ScheduleChangePersistenceAdapter implements ScheduleChangeStorePort {

    private final ScheduleChangeSetJpaRepository changeSets;
    private final ScheduleDiagnosticResolutionJpaRepository resolutions;

    ScheduleChangePersistenceAdapter(
        ScheduleChangeSetJpaRepository changeSets,
        ScheduleDiagnosticResolutionJpaRepository resolutions
    ) {
        this.changeSets = changeSets;
        this.resolutions = resolutions;
    }

    @Override
    public Optional<ChangeSetRecord> findChangeSet(UUID changeSetId) {
        return changeSets.findById(changeSetId.toString())
            .map(ScheduleChangeSetJpaEntity::toRecord);
    }

    @Override
    public ChangeSetRecord saveChangeSet(ChangeSetRecord record) {
        ScheduleChangeSetJpaEntity entity = changeSets.findById(record.id().toString())
            .map(existing -> {
                existing.apply(record);
                return existing;
            })
            .orElseGet(() -> new ScheduleChangeSetJpaEntity(record));
        return changeSets.saveAndFlush(entity).toRecord();
    }

    @Override
    public List<ResolutionRecord> findResolutions(UUID tripId, UUID dayId) {
        return resolutions.findAllByTripIdAndDayId(tripId.toString(), dayId.toString())
            .stream()
            .map(ScheduleDiagnosticResolutionJpaEntity::toRecord)
            .toList();
    }

    @Override
    public Optional<ResolutionRecord> findResolution(UUID diagnosticId) {
        return resolutions.findById(diagnosticId.toString())
            .map(ScheduleDiagnosticResolutionJpaEntity::toRecord);
    }

    @Override
    public ResolutionRecord saveResolution(ResolutionRecord record) {
        ScheduleDiagnosticResolutionJpaEntity entity = resolutions
            .findById(record.diagnosticId().toString())
            .map(existing -> {
                existing.apply(record);
                return existing;
            })
            .orElseGet(() -> new ScheduleDiagnosticResolutionJpaEntity(record));
        return resolutions.save(entity).toRecord();
    }

    @Override
    public void deleteResolution(UUID diagnosticId) {
        resolutions.deleteById(diagnosticId.toString());
    }
}
