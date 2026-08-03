package com.earthtrip.platform.adapter.out.persistence.operation;

import com.earthtrip.platform.application.port.out.OperationalStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_receipts")
class WebhookReceiptJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "provider", nullable = false, length = 40)
    private String provider;
    @Column(name = "source_event_id", nullable = false, length = 160)
    private String sourceEventId;
    @Column(name = "payload_digest", nullable = false, length = 64)
    private String payloadDigest;
    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WebhookReceiptJpaEntity() { }

    WebhookReceiptJpaEntity(OperationalStorePort.WebhookReceiptRecord record) {
        id = record.id().toString();
        provider = record.provider();
        sourceEventId = record.sourceEventId();
        payloadDigest = record.payloadDigest();
        jobId = record.jobId().toString();
        receivedAt = record.receivedAt();
    }

    OperationalStorePort.WebhookReceiptRecord record() {
        return new OperationalStorePort.WebhookReceiptRecord(
            UUID.fromString(id),
            provider,
            sourceEventId,
            payloadDigest,
            UUID.fromString(jobId),
            receivedAt
        );
    }
}
