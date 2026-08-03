package com.earthtrip.identity.adapter.out.persistence.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@IdClass(PolicyConsentId.class)
@Table(name = "policy_consents")
class PolicyConsentJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Id
    @Column(name = "policy_id", nullable = false, length = 80)
    private String policyId;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PolicyConsentJpaEntity() { }

    PolicyConsentJpaEntity(
        String userId,
        String policyId,
        String decision,
        Instant decidedAt,
        String source
    ) {
        this.userId = userId;
        this.policyId = policyId;
        apply(decision, decidedAt, source);
    }

    void apply(String newDecision, Instant newDecidedAt, String newSource) {
        decision = newDecision;
        decidedAt = newDecidedAt;
        source = newSource;
    }

    String policyId() { return policyId; }

    String decision() { return decision; }

    Instant decidedAt() { return decidedAt; }

    String source() { return source; }
}
