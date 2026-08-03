package com.earthtrip.platform.adapter.in.web.api.v1.places.by_provider_place_id;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places/{providerPlaceId}")
class PlaceByProviderIdController {

    private final ProviderProxyUseCase useCase;

    PlaceByProviderIdController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    ProviderProxyUseCase.PlaceDetail get(
        @PathVariable String providerPlaceId,
        @RequestParam(required = false) String language
    ) {
        return useCase.place(providerPlaceId, language);
    }
}
