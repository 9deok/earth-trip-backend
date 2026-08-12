package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.RoutesProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class GoogleRoutesProviderAdapter implements RoutesProviderPort {

    private static final URI ROUTES_URI =
            URI.create("https://routes.googleapis.com/directions/v2:computeRoutes");
    private static final URI MATRIX_URI =
            URI.create("https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix");
    private static final String ROUTE_FIELDS =
            String.join(
                    ",",
                    "routes.distanceMeters",
                    "routes.duration",
                    "routes.polyline.encodedPolyline");
    private static final String MATRIX_FIELDS =
            String.join(
                    ",",
                    "originIndex",
                    "destinationIndex",
                    "condition",
                    "status",
                    "distanceMeters",
                    "duration");

    private final GoogleMapsApiClient client;
    private final Clock clock;

    GoogleRoutesProviderAdapter(GoogleMapsApiClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public ProviderProxyUseCase.RouteResult route(ProviderProxyUseCase.RouteQuery query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", waypoint(query.origin()));
        body.put("destination", waypoint(query.destination()));
        if (query.waypoints() != null && !query.waypoints().isEmpty()) {
            body.put(
                    "intermediates",
                    query.waypoints().stream().map(GoogleRoutesProviderAdapter::waypoint).toList());
        }
        body.put("travelMode", googleMode(query.mode()));
        if ("DRIVING".equalsIgnoreCase(query.mode())) {
            body.put("routingPreference", "TRAFFIC_AWARE");
        }
        body.put("computeAlternativeRoutes", false);
        body.put("polylineQuality", "OVERVIEW");
        body.put("polylineEncoding", "ENCODED_POLYLINE");
        if (query.departureAt() != null) {
            body.put("departureTime", query.departureAt().toString());
        }
        JsonNode response = client.post(ROUTES_URI, body, ROUTE_FIELDS, "ROUTES_PROVIDER");
        JsonNode route = response.path("routes").path(0);
        if (route.isMissingNode()) {
            throw EarthTripException.notFound("ROUTE_NOT_FOUND", "계산 가능한 경로를 찾지 못했습니다.");
        }
        return new ProviderProxyUseCase.RouteResult(
                route.path("distanceMeters").asLong(),
                durationSeconds(route.path("duration").asText()),
                nullableText(route.path("polyline").path("encodedPolyline")),
                query.mode(),
                "GOOGLE_ROUTES",
                clock.instant());
    }

    @Override
    public ProviderProxyUseCase.MatrixResult matrix(ProviderProxyUseCase.MatrixQuery query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(
                "origins",
                query.origins().stream()
                        .map(point -> Map.of("waypoint", waypoint(point)))
                        .toList());
        body.put(
                "destinations",
                query.destinations().stream()
                        .map(point -> Map.of("waypoint", waypoint(point)))
                        .toList());
        body.put("travelMode", googleMode(query.mode()));
        if ("DRIVING".equalsIgnoreCase(query.mode())) {
            body.put("routingPreference", "TRAFFIC_AWARE");
        }
        if (query.departureAt() != null) {
            body.put("departureTime", query.departureAt().toString());
        }
        JsonNode response = client.post(MATRIX_URI, body, MATRIX_FIELDS, "ROUTES_PROVIDER");
        List<ProviderProxyUseCase.MatrixCell> cells = new ArrayList<>();
        if (response.isArray()) {
            for (JsonNode cell : response) {
                cells.add(cell(cell));
            }
        }
        return new ProviderProxyUseCase.MatrixResult(
                List.copyOf(cells), query.mode(), "GOOGLE_ROUTES", clock.instant());
    }

    private static ProviderProxyUseCase.MatrixCell cell(JsonNode value) {
        String condition = value.path("condition").asText("ROUTE_NOT_FOUND");
        JsonNode status = value.path("status");
        if (!status.isMissingNode() && status.path("code").asInt() != 0) {
            condition = "PROVIDER_ERROR_" + status.path("code").asInt();
        }
        boolean routeExists = "ROUTE_EXISTS".equals(condition);
        return new ProviderProxyUseCase.MatrixCell(
                value.path("originIndex").asInt(),
                value.path("destinationIndex").asInt(),
                routeExists && value.has("distanceMeters")
                        ? value.get("distanceMeters").asLong()
                        : null,
                routeExists && value.has("duration")
                        ? durationSeconds(value.get("duration").asText())
                        : null,
                routeExists ? "OK" : condition);
    }

    private static Map<String, Object> waypoint(ProviderProxyUseCase.RoutePoint point) {
        if (point.providerPlaceId() != null && !point.providerPlaceId().isBlank()) {
            return Map.of("placeId", point.providerPlaceId());
        }
        return Map.of(
                "location",
                Map.of(
                        "latLng",
                        Map.of(
                                "latitude", point.latitude(),
                                "longitude", point.longitude())));
    }

    private static String googleMode(String mode) {
        return switch (mode.toUpperCase(Locale.ROOT)) {
            case "BICYCLING" -> "BICYCLE";
            case "WALKING", "DRIVING", "TRANSIT" -> mode.toUpperCase(Locale.ROOT);
            default ->
                    throw EarthTripException.badRequest("INVALID_ROUTE_MODE", "지원하지 않는 이동수단입니다.");
        };
    }

    private static int durationSeconds(String value) {
        if (value == null || !value.endsWith("s")) {
            return 0;
        }
        try {
            return new BigDecimal(value.substring(0, value.length() - 1))
                    .setScale(0, RoundingMode.CEILING)
                    .intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new EarthTripException(
                    "INVALID_ROUTES_PROVIDER_RESPONSE",
                    502,
                    "Google Routes 응답의 소요시간 형식이 올바르지 않습니다.");
        }
    }

    private static String nullableText(JsonNode node) {
        String value = node.asText("");
        return value.isBlank() ? null : value;
    }
}
