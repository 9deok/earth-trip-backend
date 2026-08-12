package com.earthtrip.platform.adapter.out.persistence.export;

import com.earthtrip.platform.application.port.out.TripExportStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "trip_export_jobs")
class TripExportJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Column(name = "scopes", nullable = false, columnDefinition = "JSON")
    private String scopes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Lob
    @Column(name = "artifact", columnDefinition = "LONGBLOB")
    private byte[] artifact;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TripExportJpaEntity() {}

    TripExportJpaEntity(TripExportStorePort.ExportRecord record, String scopes) {
        id = record.id().toString();
        tripId = record.tripId().toString();
        format = record.format();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
        apply(record, scopes);
    }

    void apply(TripExportStorePort.ExportRecord record, String scopes) {
        this.scopes = scopes;
        status = record.status();
        fileName = record.fileName();
        mimeType = record.mimeType();
        artifact = record.artifact();
        checksumSha256 = record.checksumSha256();
        failureCode = record.failureCode();
        failureMessage = record.failureMessage();
        attemptCount = record.attemptCount();
        updatedAt = record.updatedAt();
    }

    String scopes() {
        return scopes;
    }

    TripExportStorePort.ExportRecord toRecord(Set<String> scopeSet) {
        return new TripExportStorePort.ExportRecord(
                UUID.fromString(id),
                UUID.fromString(tripId),
                format,
                scopeSet,
                status,
                fileName,
                mimeType,
                artifact,
                checksumSha256,
                failureCode,
                failureMessage,
                attemptCount,
                UUID.fromString(createdBy),
                createdAt,
                updatedAt,
                version);
    }
}
