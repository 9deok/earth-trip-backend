package com.earthtrip.platform.application.port.out;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.math.BigDecimal;
import java.util.List;

public interface PlacesProviderPort {

    List<ProviderProxyUseCase.PlaceSummary> search(
            String query,
            String language,
            BigDecimal nearLatitude,
            BigDecimal nearLongitude,
            int limit);

    ProviderProxyUseCase.PlaceDetail detail(String providerPlaceId, String language);
}
