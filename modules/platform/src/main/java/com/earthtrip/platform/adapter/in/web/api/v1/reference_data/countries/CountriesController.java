package com.earthtrip.platform.adapter.in.web.api.v1.reference_data.countries;

import com.earthtrip.platform.application.port.in.PlatformInfoUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference-data/countries")
class CountriesController {

    private final PlatformInfoUseCase useCase;

    CountriesController(PlatformInfoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<CountryResponse> get() {
        return useCase.countries().stream()
            .map(country -> new CountryResponse(
                country.code(), country.displayName(), country.currencyCode()
            ))
            .toList();
    }
}

record CountryResponse(String code, String displayName, String currencyCode) { }
