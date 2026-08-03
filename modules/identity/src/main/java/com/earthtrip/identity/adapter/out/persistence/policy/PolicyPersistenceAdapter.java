package com.earthtrip.identity.adapter.out.persistence.policy;

import com.earthtrip.identity.application.port.out.PolicyStorePort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PolicyPersistenceAdapter implements PolicyStorePort {

    private final PolicyDocumentJpaRepository policyRepository;
    private final PolicyConsentJpaRepository consentRepository;

    PolicyPersistenceAdapter(
        PolicyDocumentJpaRepository policyRepository,
        PolicyConsentJpaRepository consentRepository
    ) {
        this.policyRepository = policyRepository;
        this.consentRepository = consentRepository;
    }

    @Override
    public List<PolicyRecord> findActivePolicies() {
        return policyRepository.findAllByActiveTrueOrderByPublishedAtAsc().stream()
            .map(PolicyDocumentJpaEntity::toRecord)
            .toList();
    }

    @Override
    public Optional<PolicyRecord> findActivePolicy(String policyId) {
        return policyRepository.findByIdAndActiveTrue(policyId)
            .map(PolicyDocumentJpaEntity::toRecord);
    }

    @Override
    public List<ConsentRecord> findConsents(UUID userId) {
        Map<String, PolicyRecord> policies = new LinkedHashMap<>();
        findActivePolicies().forEach(policy -> policies.put(policy.id(), policy));
        return consentRepository.findAllByUserId(userId.toString()).stream()
            .filter(consent -> policies.containsKey(consent.policyId()))
            .map(consent -> new ConsentRecord(
                policies.get(consent.policyId()),
                consent.decision(),
                consent.decidedAt(),
                consent.source()
            ))
            .toList();
    }

    @Override
    public ConsentRecord saveConsent(
        UUID userId,
        PolicyRecord policy,
        String decision,
        String source,
        Instant now
    ) {
        PolicyConsentId id = new PolicyConsentId(userId.toString(), policy.id());
        PolicyConsentJpaEntity entity = consentRepository.findById(id)
            .map(existing -> {
                existing.apply(decision, now, source);
                return existing;
            })
            .orElseGet(() -> new PolicyConsentJpaEntity(
                userId.toString(),
                policy.id(),
                decision,
                now,
                source
            ));
        PolicyConsentJpaEntity saved = consentRepository.save(entity);
        return new ConsentRecord(policy, saved.decision(), saved.decidedAt(), saved.source());
    }
}
