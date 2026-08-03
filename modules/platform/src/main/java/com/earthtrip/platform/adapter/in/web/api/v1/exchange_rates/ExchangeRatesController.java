package com.earthtrip.platform.adapter.in.web.api.v1.exchange_rates;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rates")
class ExchangeRatesController {

    private final ProviderProxyUseCase useCase;

    ExchangeRatesController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    ProviderProxyUseCase.ExchangeRateResult get(
        @RequestParam String base,
        @RequestParam List<String> quote,
        @RequestParam(required = false) Instant observedAt
    ) {
        return useCase.exchangeRates(base, quote, observedAt);
    }
}
