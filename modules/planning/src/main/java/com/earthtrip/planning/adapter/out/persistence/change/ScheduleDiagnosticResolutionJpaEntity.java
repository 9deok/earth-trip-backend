package com.earthtrip.planning.adapter.out.persistence.change;

import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_diagnostic_resolutions")
class ScheduleDiagnosticResolutionJpaEntity {

    @Id
    @Column(name = "diagnostic_id", nullable = false, length = 36)
    private String diagnosticId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "day_id", nullable = false, length = 36)
    private String dayId;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "resolved_by", nullable = false, length = 36)
    private String resolvedBy;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    protected ScheduleDiagnosticResolutionJpaEntity() { }

    ScheduleDiagnosticResolutionJpaEntity(ScheduleChangeStorePort.ResolutionRecord record) {
        diagnosticId = record.diagnosticId().toString();
        apply(record);
    }

    void apply(ScheduleChangeStorePort.ResolutionRecord record) {
        tripId = record.tripId().toString();
        dayId = record.dayId().toString();
        note = record.note();
        resolvedBy = record.resolvedBy().toString();
        resolvedAt = record.resolvedAt();
    }

    ScheduleChangeStorePort.ResolutionRecord toRecord() {
        return new ScheduleChangeStorePort.ResolutionRecord(
            UUID.fromString(diagnosticId), UUID.fromString(tripId), UUID.fromString(dayId),
            note, UUID.fromString(resolvedBy), resolvedAt
        );
    }
}
