package com.earthtrip.platform.adapter.out.persistence.file;

import com.earthtrip.platform.application.port.out.FileStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
class FileJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksum;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected FileJpaEntity() {}

    FileJpaEntity(FileStorePort.FileRecord record) {
        this.id = record.id().toString();
        apply(record);
    }

    void apply(FileStorePort.FileRecord record) {
        ownerUserId = record.ownerUserId().toString();
        fileName = record.fileName();
        mimeType = record.mimeType();
        sizeBytes = record.sizeBytes();
        checksum = record.checksum();
        storageKey = record.storageKey();
        status = record.status();
        createdAt = record.createdAt();
        completedAt = record.completedAt();
        deletedAt = record.deletedAt();
    }

    FileStorePort.FileRecord toRecord() {
        return new FileStorePort.FileRecord(
                UUID.fromString(id),
                UUID.fromString(ownerUserId),
                fileName,
                mimeType,
                sizeBytes,
                checksum,
                storageKey,
                status,
                createdAt,
                completedAt,
                deletedAt,
                version);
    }
}
