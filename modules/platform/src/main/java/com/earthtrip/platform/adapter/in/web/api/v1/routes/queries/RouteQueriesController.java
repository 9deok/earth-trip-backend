package com.earthtrip.platform.adapter.in.web.api.v1.routes.queries;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes/queries")
class RouteQueriesController {

    private final ProviderProxyUseCase useCase;

    RouteQueriesController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    ProviderProxyUseCase.RouteResult post(@Valid @RequestBody RouteQueryRequest request) {
        return useCase.route(
                new ProviderProxyUseCase.RouteQuery(
                        request.origin().toPoint(),
                        request.destination().toPoint(),
                        request.waypoints() == null
                                ? List.of()
                                : request.waypoints().stream()
                                        .map(RoutePointRequest::toPoint)
                                        .toList(),
                        request.mode(),
                        request.departureAt()));
    }
}

record RouteQueryRequest(
        @NotNull @Valid RoutePointRequest origin,
        @NotNull @Valid RoutePointRequest destination,
        @Size(max = 10) @Valid List<RoutePointRequest> waypoints,
        String mode,
        Instant departureAt) {}

record RoutePointRequest(BigDecimal latitude, BigDecimal longitude, String providerPlaceId) {
    ProviderProxyUseCase.RoutePoint toPoint() {
        return new ProviderProxyUseCase.RoutePoint(latitude, longitude, providerPlaceId);
    }
}
