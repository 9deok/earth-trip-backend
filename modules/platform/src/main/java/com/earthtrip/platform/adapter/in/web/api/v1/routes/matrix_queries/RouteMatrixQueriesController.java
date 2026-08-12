package com.earthtrip.platform.adapter.in.web.api.v1.routes.matrix_queries;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes/matrix-queries")
class RouteMatrixQueriesController {

    private final ProviderProxyUseCase useCase;

    RouteMatrixQueriesController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    ProviderProxyUseCase.MatrixResult post(@Valid @RequestBody RouteMatrixQueryRequest request) {
        return useCase.routeMatrix(
                new ProviderProxyUseCase.MatrixQuery(
                        request.origins().stream().map(RouteMatrixPointRequest::toPoint).toList(),
                        request.destinations().stream()
                                .map(RouteMatrixPointRequest::toPoint)
                                .toList(),
                        request.mode(),
                        request.departureAt()));
    }
}

record RouteMatrixQueryRequest(
        @NotEmpty @Valid List<RouteMatrixPointRequest> origins,
        @NotEmpty @Valid List<RouteMatrixPointRequest> destinations,
        String mode,
        Instant departureAt) {}

record RouteMatrixPointRequest(BigDecimal latitude, BigDecimal longitude, String providerPlaceId) {
    ProviderProxyUseCase.RoutePoint toPoint() {
        return new ProviderProxyUseCase.RoutePoint(latitude, longitude, providerPlaceId);
    }
}
