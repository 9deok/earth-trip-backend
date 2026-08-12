package com.earthtrip.platform.adapter.in.web.api.v1.shared_trips.by_token;

import com.earthtrip.platform.application.port.in.SharedTripAccessUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared-trips/{token}")
class SharedTripController {

    private final SharedTripAccessUseCase useCase;

    SharedTripController(SharedTripAccessUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    SharedTripAccessUseCase.SharedTripResult get(
            @PathVariable String token,
            @RequestHeader(name = "X-Share-Session", required = false) String sessionToken) {
        return useCase.sharedTrip(token, sessionToken);
    }
}
