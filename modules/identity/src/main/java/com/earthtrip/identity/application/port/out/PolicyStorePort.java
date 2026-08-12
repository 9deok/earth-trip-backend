package com.earthtrip.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyStorePort {

    List<PolicyRecord> findActivePolicies();

    Optional<PolicyRecord> findActivePolicy(String policyId);

    List<ConsentRecord> findConsents(UUID userId);

    ConsentRecord saveConsent(
            UUID userId, PolicyRecord policy, String decision, String source, Instant now);

    record PolicyRecord(
            String id,
            String type,
            String version,
            boolean required,
            String title,
            String summary,
            String contentUrl,
            Instant publishedAt) {}

    record ConsentRecord(PolicyRecord policy, String decision, Instant decidedAt, String source) {}
}
