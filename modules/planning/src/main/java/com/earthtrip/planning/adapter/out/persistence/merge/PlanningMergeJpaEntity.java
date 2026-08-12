package com.earthtrip.planning.adapter.out.persistence.merge;

import com.earthtrip.planning.application.port.out.PlanningMergeStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "planning_resource_merges")
class PlanningMergeJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "primary_id", nullable = false, length = 36)
    private String primaryId;

    @Column(name = "duplicate_ids", nullable = false, columnDefinition = "JSON")
    private String duplicateIds;

    @Column(name = "before_snapshot", nullable = false, columnDefinition = "JSON")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", nullable = false, columnDefinition = "JSON")
    private String afterSnapshot;

    @Column(name = "added_links", nullable = false, columnDefinition = "JSON")
    private String addedLinks;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "merged_by", nullable = false, length = 36)
    private String mergedBy;

    @Column(name = "merged_at", nullable = false)
    private Instant mergedAt;

    @Column(name = "reverted_by", length = 36)
    private String revertedBy;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PlanningMergeJpaEntity() {}

    PlanningMergeJpaEntity(PlanningMergeStorePort.MergeRecord record, SerializedMerge serialized) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        resourceType = record.resourceType();
        primaryId = record.primaryId().toString();
        mergedBy = record.mergedBy().toString();
        mergedAt = record.mergedAt();
        apply(record, serialized);
    }

    void apply(PlanningMergeStorePort.MergeRecord record, SerializedMerge serialized) {
        duplicateIds = serialized.duplicateIds();
        beforeSnapshot = serialized.beforeSnapshot();
        afterSnapshot = serialized.afterSnapshot();
        addedLinks = serialized.addedLinks();
        status = record.status();
        revertedBy = record.revertedBy() == null ? null : record.revertedBy().toString();
        revertedAt = record.revertedAt();
    }

    String duplicateIds() {
        return duplicateIds;
    }

    String beforeSnapshot() {
        return beforeSnapshot;
    }

    String afterSnapshot() {
        return afterSnapshot;
    }

    String addedLinks() {
        return addedLinks;
    }

    PlanningMergeStorePort.MergeRecord toRecord(
            List<UUID> duplicates,
            Map<String, Object> before,
            Map<String, Object> after,
            List<Map<String, Object>> links) {
        return new PlanningMergeStorePort.MergeRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                resourceType,
                UUID.fromString(primaryId),
                duplicates,
                before,
                after,
                links,
                status,
                UUID.fromString(mergedBy),
                mergedAt,
                revertedBy == null ? null : UUID.fromString(revertedBy),
                revertedAt,
                version);
    }

    record SerializedMerge(
            String duplicateIds, String beforeSnapshot, String afterSnapshot, String addedLinks) {}
}
