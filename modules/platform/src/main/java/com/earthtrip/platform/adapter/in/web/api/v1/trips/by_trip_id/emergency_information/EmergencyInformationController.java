package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.emergency_information;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/emergency-information")
class EmergencyInformationController {
    private final ExternalTravelUseCase u;
    private final CurrentActor a;

    EmergencyInformationController(ExternalTravelUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @GetMapping
    List<ExternalTravelUseCase.InformationResult> get(
            @PathVariable UUID tripId, @RequestParam(required = false) String language) {
        return u.emergencyInformation(tripId, a.requireUserId(), language);
    }
}
