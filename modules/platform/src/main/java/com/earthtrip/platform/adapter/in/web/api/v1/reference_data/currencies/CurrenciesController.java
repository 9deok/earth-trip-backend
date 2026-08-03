package com.earthtrip.platform.adapter.in.web.api.v1.reference_data.currencies;

import com.earthtrip.platform.application.port.in.PlatformInfoUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference-data/currencies")
class CurrenciesController {

    private final PlatformInfoUseCase useCase;

    CurrenciesController(PlatformInfoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<CurrencyResponse> get() {
        return useCase.currencies().stream()
            .map(currency -> new CurrencyResponse(
                currency.code(), currency.fractionDigits(), currency.numericCode(), currency.displayName()
            ))
            .toList();
    }
}

record CurrencyResponse(String code, int fractionDigits, int numericCode, String displayName) { }
