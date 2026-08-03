package com.earthtrip.planning.adapter.out.persistence.resource;

import com.earthtrip.planning.application.port.out.ActivityOperationStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planning_operation_results")
class PlanningOperationResultJpaEntity {

    @Id
    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 36)
    private String resourceId;

    @Column(name = "result_json", nullable = false, columnDefinition = "JSON")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlanningOperationResultJpaEntity() { }

    PlanningOperationResultJpaEntity(
        ActivityOperationStorePort.OperationRecord record,
        String resultJson
    ) {
        operationId = record.operationId().toString();
        tripId = record.tripId().toString();
        actorId = record.actorId().toString();
        status = record.status();
        resourceType = record.resourceType();
        resourceId = record.resourceId() == null ? null : record.resourceId().toString();
        this.resultJson = resultJson;
        createdAt = record.createdAt();
    }

    UUID operationId() { return UUID.fromString(operationId); }

    UUID tripId() { return UUID.fromString(tripId); }

    UUID actorId() { return UUID.fromString(actorId); }

    String status() { return status; }

    String resourceType() { return resourceType; }

    UUID resourceId() { return resourceId == null ? null : UUID.fromString(resourceId); }

    String resultJson() { return resultJson; }

    Instant createdAt() { return createdAt; }
}
