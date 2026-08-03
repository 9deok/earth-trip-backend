package com.earthtrip.platform.adapter.out.persistence.file;

import com.earthtrip.platform.application.port.out.FileStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_links")
class FileLinkJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Column(name = "linked_by", nullable = false, length = 36)
    private String linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected FileLinkJpaEntity() { }

    FileLinkJpaEntity(FileStorePort.LinkRecord record) {
        id = record.id().toString();
        fileId = record.fileId().toString();
        tripId = record.tripId().toString();
        resourceType = record.resourceType();
        resourceId = record.resourceId().toString();
        visibility = record.visibility();
        linkedBy = record.linkedBy().toString();
        linkedAt = record.linkedAt();
    }

    FileStorePort.LinkRecord toRecord() {
        return new FileStorePort.LinkRecord(
            UUID.fromString(id), UUID.fromString(fileId), UUID.fromString(tripId),
            resourceType, UUID.fromString(resourceId), visibility,
            UUID.fromString(linkedBy), linkedAt
        );
    }
}
