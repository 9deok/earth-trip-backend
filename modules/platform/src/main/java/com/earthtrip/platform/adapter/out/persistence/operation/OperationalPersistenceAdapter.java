package com.earthtrip.platform.adapter.out.persistence.operation;

import com.earthtrip.platform.application.port.out.OperationalStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class OperationalPersistenceAdapter implements OperationalStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final OperationalJobJpaRepository jobs;
    private final WebhookReceiptJpaRepository receipts;
    private final DeadLetterJpaRepository deadLetters;
    private final AdminAuditJpaRepository audits;
    private final ObjectMapper json;

    OperationalPersistenceAdapter(
            OperationalJobJpaRepository jobs,
            WebhookReceiptJpaRepository receipts,
            DeadLetterJpaRepository deadLetters,
            AdminAuditJpaRepository audits,
            ObjectMapper json) {
        this.jobs = jobs;
        this.receipts = receipts;
        this.deadLetters = deadLetters;
        this.audits = audits;
        this.json = json;
    }

    @Override
    public Optional<WebhookReceiptRecord> webhookReceipt(String provider, String sourceEventId) {
        return receipts.findByProviderAndSourceEventId(provider, sourceEventId)
                .map(WebhookReceiptJpaEntity::record);
    }

    @Override
    public WebhookReceiptRecord saveWebhookReceipt(WebhookReceiptRecord record) {
        return receipts.saveAndFlush(new WebhookReceiptJpaEntity(record)).record();
    }

    @Override
    public Optional<JobRecord> job(UUID id) {
        return jobs.findById(id.toString()).map(this::job);
    }

    @Override
    public List<JobRecord> jobs(String status, String jobType, int limit) {
        return jobs.search(status, jobType, PageRequest.of(0, limit)).stream()
                .map(this::job)
                .toList();
    }

    @Override
    public JobRecord saveJob(JobRecord record) {
        String payload = write(record.payload());
        OperationalJobJpaEntity entity =
                jobs.findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, payload);
                                    return existing;
                                })
                        .orElseGet(() -> new OperationalJobJpaEntity(record, payload));
        return job(jobs.saveAndFlush(entity));
    }

    @Override
    public Optional<DeadLetterRecord> deadLetter(UUID id) {
        return deadLetters.findById(id.toString()).map(this::deadLetter);
    }

    @Override
    public Optional<DeadLetterRecord> openDeadLetterForJob(UUID jobId) {
        return deadLetters
                .findFirstByJobIdAndStatusOrderByCreatedAtDesc(jobId.toString(), "OPEN")
                .map(this::deadLetter);
    }

    @Override
    public List<DeadLetterRecord> deadLetters(String status, int limit) {
        return deadLetters.search(status, PageRequest.of(0, limit)).stream()
                .map(this::deadLetter)
                .toList();
    }

    @Override
    public DeadLetterRecord saveDeadLetter(DeadLetterRecord record) {
        String payload = write(record.payload());
        DeadLetterJpaEntity entity =
                deadLetters
                        .findById(record.id().toString())
                        .map(
                                existing -> {
                                    existing.apply(record, payload);
                                    return existing;
                                })
                        .orElseGet(() -> new DeadLetterJpaEntity(record, payload));
        return deadLetter(deadLetters.saveAndFlush(entity));
    }

    @Override
    public AuditRecord saveAudit(AuditRecord record) {
        return audit(
                audits.saveAndFlush(new AdminAuditJpaEntity(record, write(record.metadata()))));
    }

    @Override
    public List<AuditRecord> auditEvents(
            String action, String targetType, String targetId, int limit) {
        return audits.search(action, targetType, targetId, PageRequest.of(0, limit)).stream()
                .map(this::audit)
                .toList();
    }

    private JobRecord job(OperationalJobJpaEntity entity) {
        return entity.record(read(entity.payload()));
    }

    private DeadLetterRecord deadLetter(DeadLetterJpaEntity entity) {
        return entity.record(read(entity.payload()));
    }

    private AuditRecord audit(AdminAuditJpaEntity entity) {
        return entity.record(read(entity.metadata()));
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("운영 이벤트를 JSON으로 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 운영 이벤트 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
