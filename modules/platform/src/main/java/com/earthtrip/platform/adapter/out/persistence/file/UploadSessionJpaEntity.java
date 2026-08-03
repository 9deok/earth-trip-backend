package com.earthtrip.platform.adapter.out.persistence.file;

import com.earthtrip.platform.application.port.out.FileStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_upload_sessions")
class UploadSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "aborted_at")
    private Instant abortedAt;

    protected UploadSessionJpaEntity() { }

    UploadSessionJpaEntity(FileStorePort.UploadRecord record) {
        id = record.id().toString();
        apply(record);
    }

    void apply(FileStorePort.UploadRecord record) {
        fileId = record.fileId().toString();
        status = record.status();
        expiresAt = record.expiresAt();
        createdAt = record.createdAt();
        abortedAt = record.abortedAt();
    }

    FileStorePort.UploadRecord toRecord() {
        return new FileStorePort.UploadRecord(
            UUID.fromString(id), UUID.fromString(fileId), status, expiresAt, createdAt, abortedAt
        );
    }
}
