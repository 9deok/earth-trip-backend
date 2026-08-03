package com.earthtrip.identity.adapter.out.persistence.export;

import com.earthtrip.identity.application.port.out.DataExportStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DataExportPersistenceAdapter implements DataExportStorePort {

    private final DataExportJpaRepository repository;

    DataExportPersistenceAdapter(DataExportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ExportRecord> findAll(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId.toString()).stream()
            .map(DataExportJpaEntity::toRecord)
            .toList();
    }

    @Override
    public Optional<ExportRecord> findById(UUID exportId) {
        return repository.findById(exportId.toString()).map(DataExportJpaEntity::toRecord);
    }

    @Override
    public ExportRecord save(ExportRecord record) {
        DataExportJpaEntity entity = repository.findById(record.id().toString())
            .map(current -> {
                current.apply(record);
                return current;
            })
            .orElseGet(() -> new DataExportJpaEntity(record));
        return repository.save(entity).toRecord();
    }
}
