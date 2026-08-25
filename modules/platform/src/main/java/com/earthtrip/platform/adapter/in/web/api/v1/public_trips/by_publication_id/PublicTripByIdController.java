package com.earthtrip.platform.adapter.in.web.api.v1.public_trips.by_publication_id;

import com.earthtrip.platform.application.port.in.SharedTripAccessUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-trips/{publicationId}")
class PublicTripByIdController {

    private final SharedTripAccessUseCase useCase;

    PublicTripByIdController(SharedTripAccessUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    SharedTripAccessUseCase.SharedTripResult get(@PathVariable UUID publicationId) {
        return useCase.publicTrip(publicationId);
    }
}
