package com.earthtrip.identity.adapter.out.persistence.policy;

import com.earthtrip.identity.application.port.out.PolicyStorePort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "policy_documents")
class PolicyDocumentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Column(name = "policy_type", nullable = false, length = 40)
    private String policyType;

    @Column(name = "version_name", nullable = false, length = 40)
    private String versionName;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content_url", nullable = false, length = 500)
    private String contentUrl;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected PolicyDocumentJpaEntity() {}

    String id() {
        return id;
    }

    boolean active() {
        return active;
    }

    PolicyStorePort.PolicyRecord toRecord() {
        return new PolicyStorePort.PolicyRecord(
                id, policyType, versionName, required, title, summary, contentUrl, publishedAt);
    }
}
