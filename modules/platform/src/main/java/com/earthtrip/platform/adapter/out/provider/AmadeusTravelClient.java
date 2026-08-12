package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class AmadeusTravelClient {

    private static final Pattern FLIGHT_REFERENCE =
            Pattern.compile(
                    "^([A-Z0-9]{2,3})[ -]?(\\d{1,4})(?:[@:](\\d{4}-\\d{2}-\\d{2}))?$",
                    Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final Clock clock;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String apiSecret;
    private volatile AccessToken accessToken;

    @Autowired
    AmadeusTravelClient(
            RestClient.Builder builder,
            Clock clock,
            @Value("${earthtrip.providers.amadeus.base-url:https://test.api.amadeus.com}")
                    String apiBaseUrl,
            @Value("${earthtrip.providers.amadeus.api-key:}") String apiKey,
            @Value("${earthtrip.providers.amadeus.api-secret:}") String apiSecret) {
        this(builder.build(), clock, apiBaseUrl, apiKey, apiSecret);
    }

    AmadeusTravelClient(
            RestClient restClient,
            Clock clock,
            String apiBaseUrl,
            String apiKey,
            String apiSecret) {
        this.restClient = restClient;
        this.clock = clock;
        this.apiBaseUrl = stripSlash(apiBaseUrl);
        this.apiKey = text(apiKey);
        this.apiSecret = text(apiSecret);
    }

    boolean configured() {
        return !apiKey.isBlank() && !apiSecret.isBlank();
    }

    List<ExternalTravelUseCase.TransportStatusResult> statuses(
            List<String> references, Instant observedAt) {
        requireConfigured();
        LocalDate fallbackDate =
                LocalDate.ofInstant(
                        observedAt == null ? clock.instant() : observedAt, ZoneOffset.UTC);
        List<ExternalTravelUseCase.TransportStatusResult> results = new ArrayList<>();
        for (String reference : references) {
            Matcher matcher = FLIGHT_REFERENCE.matcher(reference.strip());
            if (!matcher.matches()) {
                throw EarthTripException.badRequest(
                        "INVALID_FLIGHT_REFERENCE", "항공편은 KE123@2026-08-03 형식으로 입력해 주세요.");
            }
            LocalDate date =
                    matcher.group(3) == null ? fallbackDate : LocalDate.parse(matcher.group(3));
            results.add(
                    status(
                            reference,
                            matcher.group(1).toUpperCase(Locale.ROOT),
                            matcher.group(2),
                            date));
        }
        return List.copyOf(results);
    }

    Map<String, Object> refreshComparison(Map<String, Object> current) {
        requireConfigured();
        String type =
                string(current, "type", string(current, "optionType", "")).toUpperCase(Locale.ROOT);
        if (!"FLIGHT".equals(type)) {
            throw EarthTripException.unavailable(
                    "COMPARISON_PROVIDER_NOT_SUPPORTED", "현재 자동 가격 갱신은 항공 비교안만 지원합니다.");
        }
        String origin = required(current, "originIata");
        String destination = required(current, "destinationIata");
        String departureDate = required(current, "departureDate");
        String currency = string(current, "currency", "KRW");
        int adults = integer(current, "adults", 1);
        UriComponentsBuilder uri =
                UriComponentsBuilder.fromUriString(apiBaseUrl + "/v2/shopping/flight-offers")
                        .queryParam("originLocationCode", origin)
                        .queryParam("destinationLocationCode", destination)
                        .queryParam("departureDate", departureDate)
                        .queryParam("adults", adults)
                        .queryParam("currencyCode", currency)
                        .queryParam("max", 5);
        String returnDate = string(current, "returnDate", null);
        if (returnDate != null) {
            uri.queryParam("returnDate", returnDate);
        }
        JsonNode response = get(uri.build().encode().toUri());
        JsonNode offer = response.path("data").path(0);
        if (offer.isMissingNode()) {
            throw EarthTripException.notFound(
                    "FLIGHT_OFFER_NOT_FOUND", "현재 조건에 맞는 항공편 가격을 찾지 못했습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "AMADEUS");
        result.put("providerOfferId", offer.path("id").asText());
        result.put("priceAmount", offer.path("price").path("grandTotal").decimalValue());
        result.put("currency", offer.path("price").path("currency").asText(currency));
        result.put("bookableSeats", offer.path("numberOfBookableSeats").asInt());
        result.put("lastTicketingDate", offer.path("lastTicketingDate").asText(null));
        result.put("itineraryCount", offer.path("itineraries").size());
        result.put("refreshedAt", clock.instant().toString());
        return Map.copyOf(result);
    }

    private ExternalTravelUseCase.TransportStatusResult status(
            String reference, String carrier, String flightNumber, LocalDate date) {
        URI uri =
                UriComponentsBuilder.fromUriString(apiBaseUrl + "/v2/schedule/flights")
                        .queryParam("carrierCode", carrier)
                        .queryParam("flightNumber", flightNumber)
                        .queryParam("scheduledDepartureDate", date)
                        .build()
                        .encode()
                        .toUri();
        JsonNode response = get(uri);
        JsonNode flight = response.path("data").path(0);
        if (flight.isMissingNode()) {
            return new ExternalTravelUseCase.TransportStatusResult(
                    reference,
                    "FLIGHT",
                    "NOT_FOUND",
                    null,
                    null,
                    "해당 날짜의 항공편 정보를 찾지 못했습니다.",
                    "AMADEUS",
                    clock.instant());
        }
        JsonNode flightPoints = flight.path("flightPoints");
        Timing departure = timing(flightPoints.path(0).path("departure").path("timings"));
        Timing arrival =
                timing(
                        flightPoints
                                .path(Math.max(0, flightPoints.size() - 1))
                                .path("arrival")
                                .path("timings"));
        Instant scheduled =
                departure.scheduled() == null ? arrival.scheduled() : departure.scheduled();
        Instant estimated =
                arrival.estimated() == null ? departure.estimated() : arrival.estimated();
        String status = deriveStatus(departure, arrival);
        return new ExternalTravelUseCase.TransportStatusResult(
                reference,
                "FLIGHT",
                status,
                scheduled,
                estimated,
                "Amadeus 실시간 운항정보",
                "AMADEUS",
                clock.instant());
    }

    private JsonNode get(URI uri) {
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri(uri)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(JsonNode.class);
            return response == null
                    ? com.fasterxml.jackson.databind.node.NullNode.instance
                    : response;
        } catch (RestClientException exception) {
            throw EarthTripException.unavailable(
                    "AMADEUS_PROVIDER_UNAVAILABLE", "Amadeus 여행정보 제공자에 연결할 수 없습니다.");
        }
    }

    private String token() {
        AccessToken current = accessToken;
        if (current != null && current.expiresAt().isAfter(clock.instant().plusSeconds(30))) {
            return current.value();
        }
        synchronized (this) {
            current = accessToken;
            if (current != null && current.expiresAt().isAfter(clock.instant().plusSeconds(30))) {
                return current.value();
            }
            String body =
                    "grant_type=client_credentials&client_id="
                            + encode(apiKey)
                            + "&client_secret="
                            + encode(apiSecret);
            try {
                JsonNode response =
                        restClient
                                .post()
                                .uri(URI.create(apiBaseUrl + "/v1/security/oauth2/token"))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .accept(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .body(JsonNode.class);
                if (response == null || response.path("access_token").asText("").isBlank()) {
                    throw invalidTokenResponse();
                }
                current =
                        new AccessToken(
                                response.path("access_token").asText(),
                                clock.instant()
                                        .plusSeconds(response.path("expires_in").asLong(1_800)));
                accessToken = current;
                return current.value();
            } catch (RestClientException exception) {
                throw EarthTripException.unavailable(
                        "AMADEUS_AUTHENTICATION_FAILED", "Amadeus API 인증에 실패했습니다.");
            }
        }
    }

    private static Timing timing(JsonNode values) {
        Instant scheduled = null;
        Instant estimated = null;
        Instant actual = null;
        for (JsonNode value : values) {
            String qualifier = value.path("qualifier").asText("");
            Instant time = instant(value.path("value").asText(null));
            switch (qualifier) {
                case "STD", "STA" -> scheduled = time;
                case "ETD", "ETA" -> estimated = time;
                case "ATD", "ATA" -> actual = time;
                default -> {}
            }
        }
        return new Timing(scheduled, estimated, actual);
    }

    private static String deriveStatus(Timing departure, Timing arrival) {
        if (arrival.actual() != null) {
            return "ARRIVED";
        }
        if (departure.actual() != null) {
            return "DEPARTED";
        }
        Instant scheduled = departure.scheduled();
        Instant estimated = departure.estimated();
        if (scheduled != null
                && estimated != null
                && estimated.isAfter(scheduled.plus(Duration.ofMinutes(15)))) {
            return "DELAYED";
        }
        return "SCHEDULED";
    }

    private void requireConfigured() {
        if (!configured()) {
            throw EarthTripException.unavailable(
                    "AMADEUS_PROVIDER_NOT_CONFIGURED", "Amadeus API 자격증명이 설정되지 않았습니다.");
        }
    }

    private static String required(Map<String, Object> values, String key) {
        String value = string(values, key, null);
        if (value == null) {
            throw EarthTripException.badRequest(
                    "COMPARISON_FIELD_REQUIRED", "항공 가격 갱신에 " + key + " 값이 필요합니다.");
        }
        return value;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString().strip();
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Instant instant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripSlash(String value) {
        String result =
                value == null || value.isBlank() ? "https://test.api.amadeus.com" : value.strip();
        return result.replaceAll("/+$", "");
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }

    private static EarthTripException invalidTokenResponse() {
        return new EarthTripException(
                "INVALID_AMADEUS_TOKEN_RESPONSE", 502, "Amadeus 인증 응답을 해석할 수 없습니다.");
    }

    private record AccessToken(String value, Instant expiresAt) {}

    private record Timing(Instant scheduled, Instant estimated, Instant actual) {}
}
