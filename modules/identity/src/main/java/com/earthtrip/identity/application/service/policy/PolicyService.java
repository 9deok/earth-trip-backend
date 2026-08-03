package com.earthtrip.identity.application.service.policy;

import com.earthtrip.identity.application.port.in.PolicyUseCase;
import com.earthtrip.identity.application.port.out.PolicyStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PolicyService implements PolicyUseCase {

    private final PolicyStorePort store;
    private final Clock clock;

    PolicyService(PolicyStorePort store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResult> currentPolicies() {
        return store.findActivePolicies().stream().map(PolicyService::policyResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentResult> consents(UUID userId) {
        return store.findConsents(userId).stream().map(PolicyService::consentResult).toList();
    }

    @Override
    public ConsentResult decide(UUID userId, String policyId, String rawDecision, String rawSource) {
        PolicyStorePort.PolicyRecord policy = store.findActivePolicy(policyId)
            .orElseThrow(() -> EarthTripException.notFound("POLICY_NOT_FOUND", "정책을 찾을 수 없습니다."));
        String decision = normalizeDecision(rawDecision);
        if (policy.required() && !decision.equals("ACCEPTED")) {
            throw EarthTripException.badRequest(
                "REQUIRED_POLICY_CANNOT_BE_DECLINED",
                "필수 정책은 동의 철회 대신 계정 탈퇴 절차를 사용해야 합니다."
            );
        }
        String source = rawSource == null || rawSource.isBlank()
            ? "APP"
            : rawSource.strip().toUpperCase(Locale.ROOT);
        return consentResult(store.saveConsent(
            userId,
            policy,
            decision,
            source,
            clock.instant()
        ));
    }

    private static String normalizeDecision(String rawDecision) {
        if (rawDecision == null) {
            throw EarthTripException.badRequest("POLICY_DECISION_REQUIRED", "동의 여부가 필요합니다.");
        }
        String decision = rawDecision.strip().toUpperCase(Locale.ROOT);
        if (!decision.equals("ACCEPTED") && !decision.equals("DECLINED")) {
            throw EarthTripException.badRequest(
                "INVALID_POLICY_DECISION",
                "정책 결정은 ACCEPTED 또는 DECLINED여야 합니다."
            );
        }
        return decision;
    }

    private static PolicyResult policyResult(PolicyStorePort.PolicyRecord policy) {
        return new PolicyResult(
            policy.id(),
            policy.type(),
            policy.version(),
            policy.required(),
            policy.title(),
            policy.summary(),
            policy.contentUrl(),
            policy.publishedAt()
        );
    }

    private static ConsentResult consentResult(PolicyStorePort.ConsentRecord consent) {
        PolicyStorePort.PolicyRecord policy = consent.policy();
        return new ConsentResult(
            policy.id(),
            policy.type(),
            policy.version(),
            policy.required(),
            consent.decision(),
            consent.decidedAt(),
            consent.source()
        );
    }
}
