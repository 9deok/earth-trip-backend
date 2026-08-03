package com.earthtrip.platform.application.port.out;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.time.Instant;
import java.util.List;

public interface ExchangeRateProviderPort {

    ProviderProxyUseCase.ExchangeRateResult rates(
        String baseCurrency,
        List<String> quoteCurrencies,
        Instant observedAt
    );
}
