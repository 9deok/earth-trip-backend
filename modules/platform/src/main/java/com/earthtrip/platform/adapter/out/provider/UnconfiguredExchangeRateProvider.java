package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.ExchangeRateProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredExchangeRateProvider implements ExchangeRateProviderPort {

    @Override
    public ProviderProxyUseCase.ExchangeRateResult rates(
        String baseCurrency,
        List<String> quoteCurrencies,
        Instant observedAt
    ) {
        throw EarthTripException.unavailable(
            "EXCHANGE_RATE_PROVIDER_NOT_CONFIGURED",
            "환율 제공자가 설정되지 않았습니다."
        );
    }
}
