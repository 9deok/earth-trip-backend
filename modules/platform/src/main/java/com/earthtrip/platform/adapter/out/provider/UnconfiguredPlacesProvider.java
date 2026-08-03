package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.PlacesProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredPlacesProvider implements PlacesProviderPort {

    @Override
    public List<ProviderProxyUseCase.PlaceSummary> search(
        String query,
        String language,
        BigDecimal nearLatitude,
        BigDecimal nearLongitude,
        int limit
    ) {
        throw unavailable();
    }

    @Override
    public ProviderProxyUseCase.PlaceDetail detail(String providerPlaceId, String language) {
        throw unavailable();
    }

    private static EarthTripException unavailable() {
        return EarthTripException.unavailable(
            "PLACES_PROVIDER_NOT_CONFIGURED",
            "장소 검색 제공자가 설정되지 않았습니다."
        );
    }
}
