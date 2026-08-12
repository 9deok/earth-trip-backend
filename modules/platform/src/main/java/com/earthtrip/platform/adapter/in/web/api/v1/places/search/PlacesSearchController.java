package com.earthtrip.platform.adapter.in.web.api.v1.places.search;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places/search")
class PlacesSearchController {

    private final ProviderProxyUseCase useCase;

    PlacesSearchController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<ProviderProxyUseCase.PlaceSummary> get(
            @RequestParam String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) BigDecimal nearLatitude,
            @RequestParam(required = false) BigDecimal nearLongitude,
            @RequestParam(required = false) Integer limit) {
        return useCase.searchPlaces(q, language, nearLatitude, nearLongitude, limit);
    }
}
