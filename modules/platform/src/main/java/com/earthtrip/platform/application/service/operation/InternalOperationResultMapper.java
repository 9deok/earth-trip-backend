package com.earthtrip.platform.application.service.operation;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import com.earthtrip.platform.application.port.out.OperationalStorePort;

final class InternalOperationResultMapper {

    private InternalOperationResultMapper() {}

    static InternalOperationsUseCase.WebhookResult webhook(
            OperationalStorePort.JobRecord job,
            OperationalStorePort.WebhookReceiptRecord receipt,
            boolean duplicate) {
        return new InternalOperationsUseCase.WebhookResult(
                job.id(),
                receipt.provider(),
                receipt.sourceEventId(),
                job.status(),
                duplicate,
                receipt.receivedAt());
    }

    static InternalOperationsUseCase.JobResult job(OperationalStorePort.JobRecord record) {
        return new InternalOperationsUseCase.JobResult(
                record.id(),
                record.jobType(),
                record.sourceEventId(),
                record.status(),
                record.payload(),
                record.attemptCount(),
                record.availableAt(),
                record.createdAt(),
                record.updatedAt(),
                record.completedAt(),
                record.errorCode(),
                record.errorMessage(),
                record.version());
    }

    static InternalOperationsUseCase.DeadLetterResult deadLetter(
            OperationalStorePort.DeadLetterRecord record) {
        return new InternalOperationsUseCase.DeadLetterResult(
                record.id(),
                record.jobId(),
                record.eventType(),
                record.payload(),
                record.errorCode(),
                record.errorMessage(),
                record.status(),
                record.createdAt(),
                record.replayedAt(),
                record.version());
    }

    static InternalOperationsUseCase.AuditResult audit(OperationalStorePort.AuditRecord record) {
        return new InternalOperationsUseCase.AuditResult(
                record.sequenceId(),
                record.eventId(),
                record.actorType(),
                record.actorId(),
                record.action(),
                record.targetType(),
                record.targetId(),
                record.outcome(),
                record.metadata(),
                record.occurredAt());
    }
}
