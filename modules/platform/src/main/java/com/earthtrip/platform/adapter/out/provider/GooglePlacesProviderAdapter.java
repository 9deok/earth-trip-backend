package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.PlacesProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GooglePlacesProviderAdapter implements PlacesProviderPort {

    private static final URI SEARCH_URI =
            URI.create("https://places.googleapis.com/v1/places:searchText");
    private static final String SEARCH_FIELDS =
            String.join(
                    ",",
                    "places.id",
                    "places.displayName",
                    "places.formattedAddress",
                    "places.addressComponents",
                    "places.location",
                    "places.types");
    private static final String DETAIL_FIELDS =
            String.join(
                    ",",
                    "id",
                    "displayName",
                    "formattedAddress",
                    "addressComponents",
                    "location",
                    "types",
                    "regularOpeningHours",
                    "websiteUri",
                    "internationalPhoneNumber");
    private static final List<String> DAYS =
            List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");

    private final GoogleMapsApiClient client;
    private final Clock clock;

    GooglePlacesProviderAdapter(GoogleMapsApiClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public List<ProviderProxyUseCase.PlaceSummary> search(
            String query,
            String language,
            BigDecimal nearLatitude,
            BigDecimal nearLongitude,
            int limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", language);
        body.put("pageSize", limit);
        if (nearLatitude != null && nearLongitude != null) {
            body.put(
                    "locationBias",
                    Map.of(
                            "circle",
                            Map.of(
                                    "center",
                                    Map.of(
                                            "latitude", nearLatitude,
                                            "longitude", nearLongitude),
                                    "radius",
                                    25_000)));
        }
        JsonNode response = client.post(SEARCH_URI, body, SEARCH_FIELDS, "PLACES_PROVIDER");
        List<ProviderProxyUseCase.PlaceSummary> results = new ArrayList<>();
        for (JsonNode place : response.path("places")) {
            results.add(summary(place));
        }
        return List.copyOf(results);
    }

    @Override
    public ProviderProxyUseCase.PlaceDetail detail(String providerPlaceId, String language) {
        String safeId = validatePlaceId(providerPlaceId);
        URI uri =
                UriComponentsBuilder.fromUriString(
                                "https://places.googleapis.com/v1/places/{placeId}")
                        .queryParam("languageCode", language)
                        .buildAndExpand(safeId)
                        .encode()
                        .toUri();
        JsonNode place = client.get(uri, DETAIL_FIELDS, "PLACES_PROVIDER");
        ProviderProxyUseCase.PlaceSummary summary = summary(place);
        return new ProviderProxyUseCase.PlaceDetail(
                summary.providerPlaceId(),
                summary.name(),
                summary.formattedAddress(),
                summary.countryCode(),
                summary.latitude(),
                summary.longitude(),
                summary.categories(),
                openingHours(place.path("regularOpeningHours").path("periods")),
                textOrNull(place, "websiteUri"),
                textOrNull(place, "internationalPhoneNumber"),
                "GOOGLE_PLACES",
                clock.instant());
    }

    private static ProviderProxyUseCase.PlaceSummary summary(JsonNode place) {
        JsonNode location = place.path("location");
        return new ProviderProxyUseCase.PlaceSummary(
                place.path("id").asText(),
                place.path("displayName").path("text").asText(),
                place.path("formattedAddress").asText(),
                countryCode(place.path("addressComponents")),
                decimal(location, "latitude"),
                decimal(location, "longitude"),
                strings(place.path("types")),
                "GOOGLE_PLACES");
    }

    private static Map<String, List<ProviderProxyUseCase.OpeningInterval>> openingHours(
            JsonNode periods) {
        Map<String, List<ProviderProxyUseCase.OpeningInterval>> result = new LinkedHashMap<>();
        for (JsonNode period : periods) {
            JsonNode open = period.path("open");
            JsonNode close = period.path("close");
            int openDay = open.path("day").asInt(-1);
            if (openDay < 0 || openDay >= DAYS.size()) {
                continue;
            }
            int openMinute = open.path("hour").asInt() * 60 + open.path("minute").asInt();
            int closeMinute =
                    close.isMissingNode()
                            ? 24 * 60
                            : close.path("hour").asInt() * 60 + close.path("minute").asInt();
            result.computeIfAbsent(DAYS.get(openDay), ignored -> new ArrayList<>())
                    .add(new ProviderProxyUseCase.OpeningInterval(openMinute, closeMinute));
        }
        Map<String, List<ProviderProxyUseCase.OpeningInterval>> immutable = new LinkedHashMap<>();
        result.forEach((day, intervals) -> immutable.put(day, List.copyOf(intervals)));
        return Map.copyOf(immutable);
    }

    private static String validatePlaceId(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 500
                || !value.matches("[A-Za-z0-9_\\-]+")) {
            throw EarthTripException.badRequest("INVALID_PLACE_ID", "Google 장소 ID 형식을 확인해 주세요.");
        }
        return value;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.has(field) && node.get(field).isNumber()
                ? node.get(field).decimalValue()
                : null;
    }

    private static List<String> strings(JsonNode values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }

    private static String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value;
    }

    private static String countryCode(JsonNode components) {
        for (JsonNode component : components) {
            if (strings(component.path("types")).contains("country")) {
                String code = component.path("shortText").asText("").strip().toUpperCase();
                return code.matches("[A-Z]{2}") ? code : null;
            }
        }
        return null;
    }
}
