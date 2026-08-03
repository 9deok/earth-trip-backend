package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.RoutesProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredRoutesProvider implements RoutesProviderPort {

    @Override
    public ProviderProxyUseCase.RouteResult route(ProviderProxyUseCase.RouteQuery query) {
        throw unavailable();
    }

    @Override
    public ProviderProxyUseCase.MatrixResult matrix(ProviderProxyUseCase.MatrixQuery query) {
        throw unavailable();
    }

    private static EarthTripException unavailable() {
        return EarthTripException.unavailable(
            "ROUTES_PROVIDER_NOT_CONFIGURED",
            "경로 계산 제공자가 설정되지 않았습니다."
        );
    }
}
