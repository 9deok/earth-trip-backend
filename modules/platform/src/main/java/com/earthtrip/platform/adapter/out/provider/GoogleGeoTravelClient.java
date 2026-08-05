package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleGeoTravelClient {

    private static final int MAX_REDIRECTS = 5;

    private final GoogleMapsApiClient client;
    private final HttpClient redirectClient;
    private final Clock clock;

    @Autowired
    GoogleGeoTravelClient(GoogleMapsApiClient client, Clock clock) {
        this(
            client,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
            clock
        );
    }

    GoogleGeoTravelClient(
        GoogleMapsApiClient client,
        HttpClient redirectClient,
        Clock clock
    ) {
        this.client = client;
        this.redirectClient = redirectClient;
        this.clock = clock;
    }

    List<ExternalTravelUseCase.GeoResult> forward(String query, String language, int limit) {
        URI uri = UriComponentsBuilder
            .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
            .queryParam("address", query)
            .queryParam("language", language)
            .build()
            .encode()
            .toUri();
        return geocode(uri).stream().limit(limit).toList();
    }

    ExternalTravelUseCase.GeoResult reverse(
        BigDecimal latitude,
        BigDecimal longitude,
        String language
    ) {
        URI uri = UriComponentsBuilder
            .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
            .queryParam("latlng", latitude + "," + longitude)
            .queryParam("language", language)
            .build()
            .encode()
            .toUri();
        return geocode(uri).stream().findFirst().orElseThrow(() ->
            EarthTripException.notFound("GEOCODING_RESULT_NOT_FOUND", "주소를 찾지 못했습니다.")
        );
    }

    ExternalTravelUseCase.TimeZoneResult timeZone(BigDecimal latitude, BigDecimal longitude) {
        URI uri = UriComponentsBuilder
            .fromUriString("https://maps.googleapis.com/maps/api/timezone/json")
            .queryParam("location", latitude + "," + longitude)
            .queryParam("timestamp", clock.instant().getEpochSecond())
            .build()
            .encode()
            .toUri();
        JsonNode response = client.getLegacy(uri, "TIME_ZONE_PROVIDER");
        requireOk(response, "TIME_ZONE_RESULT_NOT_FOUND", "시간대를 찾지 못했습니다.");
        return new ExternalTravelUseCase.TimeZoneResult(
            response.path("timeZoneId").asText(),
            response.path("timeZoneName").asText(),
            "GOOGLE_TIME_ZONE"
        );
    }

    ExternalTravelUseCase.PlaceUrlResult resolve(String rawUrl, String language) {
        URI canonical = followGoogleRedirects(URI.create(rawUrl));
        Map<String, String> query = queryParameters(canonical);
        List<ExternalTravelUseCase.GeoResult> results;
        String placeId = query.get("query_place_id");
        if (placeId == null) {
            placeId = query.get("place_id");
        }
        if (placeId != null && !placeId.isBlank()) {
            URI uri = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("place_id", placeId)
                .queryParam("language", language)
                .build()
                .encode()
                .toUri();
            results = geocode(uri);
        } else {
            String search = firstNonBlank(
                query.get("query"),
                query.get("q"),
                query.get("destination"),
                placeNameFromPath(canonical)
            );
            if (search == null) {
                BigDecimal[] coordinates = coordinatesFromPath(canonical);
                if (coordinates == null) {
                    throw EarthTripException.badRequest(
                        "UNSUPPORTED_GOOGLE_MAPS_URL",
                        "장소를 식별할 수 있는 Google Maps 링크가 아닙니다."
                    );
                }
                results = List.of(reverse(coordinates[0], coordinates[1], language));
            } else {
                results = forward(search, language, 1);
            }
        }
        ExternalTravelUseCase.GeoResult place = results.stream().findFirst().orElseThrow(() ->
            EarthTripException.notFound("PLACE_URL_RESULT_NOT_FOUND", "링크의 장소를 찾지 못했습니다.")
        );
        return new ExternalTravelUseCase.PlaceUrlResult(
            canonical.toString(),
            place.providerPlaceId(),
            placeNameFromPath(canonical),
            place.formattedAddress(),
            place.latitude(),
            place.longitude(),
            "GOOGLE_MAPS_URL"
        );
    }

    private List<ExternalTravelUseCase.GeoResult> geocode(URI uri) {
        JsonNode response = client.getLegacy(uri, "GEOCODING_PROVIDER");
        requireOk(response, "GEOCODING_RESULT_NOT_FOUND", "주소를 찾지 못했습니다.");
        List<ExternalTravelUseCase.GeoResult> results = new ArrayList<>();
        for (JsonNode item : response.path("results")) {
            JsonNode location = item.path("geometry").path("location");
            results.add(new ExternalTravelUseCase.GeoResult(
                item.path("formatted_address").asText(),
                decimal(location, "lat"),
                decimal(location, "lng"),
                item.path("place_id").asText(),
                "GOOGLE_GEOCODING"
            ));
        }
        return List.copyOf(results);
    }

    private URI followGoogleRedirects(URI start) {
        URI current = start;
        for (int count = 0; count <= MAX_REDIRECTS; count++) {
            requireGoogleMapsHost(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "EarthTrip-PlaceResolver/1.0")
                .GET()
                .build();
            HttpResponse<Void> response;
            try {
                response = redirectClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw providerUnavailable();
            } catch (IOException exception) {
                throw providerUnavailable();
            }
            if (response.statusCode() < 300 || response.statusCode() >= 400) {
                return current;
            }
            String location = response.headers().firstValue("Location").orElseThrow(() ->
                new EarthTripException(
                    "INVALID_GOOGLE_MAPS_REDIRECT",
                    502,
                    "Google Maps 링크 이동 주소가 없습니다."
                )
            );
            current = current.resolve(location).normalize();
        }
        throw new EarthTripException(
            "TOO_MANY_GOOGLE_MAPS_REDIRECTS",
            502,
            "Google Maps 링크 이동 횟수가 너무 많습니다."
        );
    }

    private static void requireGoogleMapsHost(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!(host.equals("maps.app.goo.gl")
            || host.equals("goo.gl")
            || host.equals("maps.google.com")
            || host.startsWith("maps.google."))
            || uri.getUserInfo() != null
            || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw EarthTripException.badRequest(
                "UNSUPPORTED_PLACE_URL_PROVIDER",
                "Google Maps 장소 링크만 자동 해석할 수 있습니다."
            );
        }
    }

    private static Map<String, String> queryParameters(URI uri) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        for (String pair : raw.split("&")) {
            String[] entry = pair.split("=", 2);
            String key = decode(entry[0]);
            String value = entry.length == 2 ? decode(entry[1]) : "";
            result.putIfAbsent(key, value);
        }
        return Map.copyOf(result);
    }

    private static String placeNameFromPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null) {
            return null;
        }
        int marker = path.indexOf("/place/");
        if (marker < 0) {
            return null;
        }
        String value = path.substring(marker + "/place/".length()).split("/", 2)[0];
        value = decode(value).replace('+', ' ').strip();
        return value.isBlank() ? null : value;
    }

    private static BigDecimal[] coordinatesFromPath(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("@(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)")
            .matcher(path);
        return matcher.find()
            ? new BigDecimal[] {new BigDecimal(matcher.group(1)), new BigDecimal(matcher.group(2))}
            : null;
    }

    private static void requireOk(JsonNode response, String emptyCode, String emptyMessage) {
        String status = response.path("status").asText("OK");
        if ("ZERO_RESULTS".equals(status)) {
            throw EarthTripException.notFound(emptyCode, emptyMessage);
        }
        if (!"OK".equals(status)) {
            throw new EarthTripException(
                "GOOGLE_MAPS_PROVIDER_REJECTED",
                502,
                "Google Maps Platform이 요청을 처리하지 못했습니다."
            );
        }
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.has(field) && node.get(field).isNumber()
            ? node.get(field).decimalValue()
            : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static EarthTripException providerUnavailable() {
        return EarthTripException.unavailable(
            "GOOGLE_MAPS_LINK_PROVIDER_UNAVAILABLE",
            "Google Maps 링크를 해석할 수 없습니다."
        );
    }
}
