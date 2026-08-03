package com.earthtrip.expense.adapter.out.persistence.settlement;

import com.earthtrip.expense.application.port.out.SettlementStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_supplements")
class SettlementSupplementJpaEntity {

    @Id @Column(name = "id", nullable = false, length = 36)
    private String id;
    @Column(name = "original_settlement_id", nullable = false, length = 36)
    private String originalSettlementId;
    @Column(name = "supplement_settlement_id", nullable = false, length = 36)
    private String supplementSettlementId;
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SettlementSupplementJpaEntity() { }

    SettlementSupplementJpaEntity(SettlementStorePort.SupplementRecord record) {
        id = record.id().toString();
        originalSettlementId = record.originalSettlementId().toString();
        supplementSettlementId = record.supplementSettlementId().toString();
        createdBy = record.createdBy().toString();
        createdAt = record.createdAt();
    }

    SettlementStorePort.SupplementRecord toRecord() {
        return new SettlementStorePort.SupplementRecord(
            UUID.fromString(id), UUID.fromString(originalSettlementId),
            UUID.fromString(supplementSettlementId), UUID.fromString(createdBy), createdAt
        );
    }
}
