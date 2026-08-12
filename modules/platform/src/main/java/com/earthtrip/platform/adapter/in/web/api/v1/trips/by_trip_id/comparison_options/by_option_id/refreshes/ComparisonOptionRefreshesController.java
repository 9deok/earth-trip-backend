package com.earthtrip.platform.adapter.in.web.api.v1.trips.by_trip_id.comparison_options.by_option_id.refreshes;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/comparison-options/{optionId}/refreshes")
class ComparisonOptionRefreshesController {
    private final ExternalTravelUseCase u;
    private final CurrentActor a;

    ComparisonOptionRefreshesController(ExternalTravelUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @PostMapping
    ExternalTravelUseCase.ComparisonRefreshResult post(
            @PathVariable UUID tripId,
            @PathVariable UUID optionId,
            @RequestParam @PositiveOrZero long baseVersion) {
        return u.refreshComparison(tripId, optionId, a.requireUserId(), baseVersion);
    }
}
