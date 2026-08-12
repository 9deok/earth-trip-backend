package com.earthtrip.identity.adapter.in.web.api.v1.policies.current;

import com.earthtrip.identity.application.port.in.PolicyUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies/current")
class CurrentPoliciesController {

    private final PolicyUseCase useCase;

    CurrentPoliciesController(PolicyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<CurrentPolicyResponse> get() {
        return useCase.currentPolicies().stream().map(CurrentPoliciesController::response).toList();
    }

    private static CurrentPolicyResponse response(PolicyUseCase.PolicyResult result) {
        return new CurrentPolicyResponse(
                result.policyId(),
                result.type(),
                result.version(),
                result.required(),
                result.title(),
                result.summary(),
                result.contentUrl(),
                result.publishedAt());
    }
}

record CurrentPolicyResponse(
        String policyId,
        String type,
        String version,
        boolean required,
        String title,
        String summary,
        String contentUrl,
        Instant publishedAt) {}
