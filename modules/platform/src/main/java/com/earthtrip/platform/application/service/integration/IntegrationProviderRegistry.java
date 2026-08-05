package com.earthtrip.platform.application.service.integration;

import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class IntegrationProviderRegistry {

    private final List<ExternalAccountProviderPort> providers;

    IntegrationProviderRegistry(List<ExternalAccountProviderPort> providers) {
        this.providers = List.copyOf(providers);
    }

    ExternalAccountProviderPort require(String provider) {
        return find(provider).orElseThrow(() -> EarthTripException.badRequest(
            "UNSUPPORTED_INTEGRATION_PROVIDER",
            "지원하지 않는 외부 계정 제공자입니다."
        ));
    }

    Optional<ExternalAccountProviderPort> find(String provider) {
        return providers.stream().filter(candidate -> candidate.supports(provider)).findFirst();
    }

    boolean configured(String provider) {
        return find(provider).map(ExternalAccountProviderPort::configured).orElse(false);
    }
}
