package com.earthtrip.identity.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PolicyUseCase {

    List<PolicyResult> currentPolicies();

    List<ConsentResult> consents(UUID userId);

    ConsentResult decide(UUID userId, String policyId, String decision, String source);

    record PolicyResult(
            String policyId,
            String type,
            String version,
            boolean required,
            String title,
            String summary,
            String contentUrl,
            Instant publishedAt) {}

    record ConsentResult(
            String policyId,
            String policyType,
            String policyVersion,
            boolean required,
            String decision,
            Instant decidedAt,
            String source) {}
}
