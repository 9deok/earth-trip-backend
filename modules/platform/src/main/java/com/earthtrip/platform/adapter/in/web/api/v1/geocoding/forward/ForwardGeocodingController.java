package com.earthtrip.platform.adapter.in.web.api.v1.geocoding.forward;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/geocoding/forward")
class ForwardGeocodingController {
    private final ExternalTravelUseCase u;

    ForwardGeocodingController(ExternalTravelUseCase u) {
        this.u = u;
    }

    @GetMapping
    List<ExternalTravelUseCase.GeoResult> get(
            @RequestParam String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer limit) {
        return u.forward(q, language, limit);
    }
}
