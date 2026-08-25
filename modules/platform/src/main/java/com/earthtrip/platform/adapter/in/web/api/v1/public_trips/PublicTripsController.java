package com.earthtrip.platform.adapter.in.web.api.v1.public_trips;

import com.earthtrip.platform.application.port.in.PublicTripDiscoveryUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public-trips")
class PublicTripsController {

    private final PublicTripDiscoveryUseCase useCase;

    PublicTripsController(PublicTripDiscoveryUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<PublicTripDiscoveryUseCase.PublicTripSummary> get(
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        return useCase.discover(destination, limit);
    }
}
