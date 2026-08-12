package com.earthtrip.identity.adapter.out.persistence.export;

import com.earthtrip.identity.application.port.out.DataExportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_export_jobs")
class DataExportJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "format", nullable = false, length = 30)
    private String format;

    @Column(name = "file_id", length = 36)
    private String fileId;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected DataExportJpaEntity() {}

    DataExportJpaEntity(DataExportStorePort.ExportRecord record) {
        id = record.id().toString();
        apply(record);
    }

    void apply(DataExportStorePort.ExportRecord record) {
        userId = record.userId().toString();
        status = record.status();
        format = record.format();
        fileId = record.fileId() == null ? null : record.fileId().toString();
        errorCode = record.errorCode();
        createdAt = record.createdAt();
        completedAt = record.completedAt();
        expiresAt = record.expiresAt();
    }

    DataExportStorePort.ExportRecord toRecord() {
        return new DataExportStorePort.ExportRecord(
                UUID.fromString(id),
                UUID.fromString(userId),
                status,
                format,
                fileId == null ? null : UUID.fromString(fileId),
                errorCode,
                createdAt,
                completedAt,
                expiresAt);
    }
}
