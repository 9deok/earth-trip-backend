package com.earthtrip.trip.adapter.out.persistence.structure;

import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_structure_diagnostic_resolutions")
class DiagnosticResolutionJpaEntity {

    @Id
    @Column(name = "diagnostic_id", nullable = false, length = 36)
    private String diagnosticId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "resolved_by", nullable = false, length = 36)
    private String resolvedBy;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    protected DiagnosticResolutionJpaEntity() { }

    DiagnosticResolutionJpaEntity(TripStructureStorePort.ResolutionRecord record) {
        diagnosticId = record.diagnosticId().toString();
        apply(record);
    }

    void apply(TripStructureStorePort.ResolutionRecord record) {
        tripId = record.tripId().toString();
        note = record.note();
        resolvedBy = record.resolvedBy().toString();
        resolvedAt = record.resolvedAt();
    }

    TripStructureStorePort.ResolutionRecord toRecord() {
        return new TripStructureStorePort.ResolutionRecord(
            UUID.fromString(diagnosticId), UUID.fromString(tripId), note,
            UUID.fromString(resolvedBy), resolvedAt
        );
    }
}
