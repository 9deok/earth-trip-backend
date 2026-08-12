package com.earthtrip.identity.adapter.in.web.api.v1.me.policy_consents;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.PolicyUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/policy-consents")
class PolicyConsentsController {

    private final PolicyUseCase useCase;
    private final CurrentUserProvider currentUser;

    PolicyConsentsController(PolicyUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<PolicyConsentResponse> get() {
        return useCase.consents(currentUser.requireUserId()).stream()
                .map(PolicyConsentsController::response)
                .toList();
    }

    private static PolicyConsentResponse response(PolicyUseCase.ConsentResult result) {
        return new PolicyConsentResponse(
                result.policyId(),
                result.policyType(),
                result.policyVersion(),
                result.required(),
                result.decision(),
                result.decidedAt(),
                result.source());
    }
}

record PolicyConsentResponse(
        String policyId,
        String policyType,
        String policyVersion,
        boolean required,
        String decision,
        Instant decidedAt,
        String source) {}
