package com.earthtrip.identity.adapter.in.web.api.v1.me.policy_consents.by_policy_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.PolicyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/policy-consents/{policyId}")
class PolicyConsentByIdController {

    private final PolicyUseCase useCase;
    private final CurrentUserProvider currentUser;

    PolicyConsentByIdController(PolicyUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @PutMapping
    PolicyConsentResponse put(
        @PathVariable String policyId,
        @Valid @RequestBody PolicyConsentRequest request
    ) {
        PolicyUseCase.ConsentResult result = useCase.decide(
            currentUser.requireUserId(), policyId, request.decision(), request.source()
        );
        return new PolicyConsentResponse(
            result.policyId(), result.policyType(), result.policyVersion(), result.required(),
            result.decision(), result.decidedAt(), result.source()
        );
    }
}

record PolicyConsentRequest(@NotBlank String decision, @NotBlank String source) { }

record PolicyConsentResponse(
    String policyId,
    String policyType,
    String policyVersion,
    boolean required,
    String decision,
    Instant decidedAt,
    String source
) { }
