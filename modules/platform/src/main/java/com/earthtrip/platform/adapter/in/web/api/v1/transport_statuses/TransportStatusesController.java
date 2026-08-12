package com.earthtrip.platform.adapter.in.web.api.v1.transport_statuses;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transport-statuses")
class TransportStatusesController {
    private final ExternalTravelUseCase u;

    TransportStatusesController(ExternalTravelUseCase u) {
        this.u = u;
    }

    @GetMapping
    List<ExternalTravelUseCase.TransportStatusResult> get(
            @RequestParam List<String> reference,
            @RequestParam(required = false) Instant observedAt) {
        return u.transportStatuses(reference, observedAt);
    }
}
