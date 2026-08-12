package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleMapsApiClient {

    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    GoogleMapsApiClient(
            RestClient.Builder builder,
            @Value("${earthtrip.providers.google-maps.api-key:}") String apiKey) {
        this(builder.build(), apiKey);
    }

    GoogleMapsApiClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
    }

    boolean configured() {
        return !apiKey.isBlank();
    }

    JsonNode get(URI uri, String fieldMask, String providerCode) {
        requireConfigured(providerCode);
        try {
            RestClient.RequestHeadersSpec<?> request =
                    restClient
                            .get()
                            .uri(uri)
                            .header(API_KEY_HEADER, apiKey)
                            .accept(MediaType.APPLICATION_JSON);
            if (fieldMask != null && !fieldMask.isBlank()) {
                request = request.header(FIELD_MASK_HEADER, fieldMask);
            }
            JsonNode response = request.retrieve().body(JsonNode.class);
            return response == null
                    ? com.fasterxml.jackson.databind.node.NullNode.instance
                    : response;
        } catch (RestClientResponseException exception) {
            throw rejected(providerCode, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw unavailable(providerCode);
        }
    }

    JsonNode getLegacy(URI uri, String providerCode) {
        requireConfigured(providerCode);
        URI authenticatedUri =
                UriComponentsBuilder.fromUri(uri)
                        .queryParam("key", apiKey)
                        .build()
                        .encode()
                        .toUri();
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri(authenticatedUri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(JsonNode.class);
            return response == null
                    ? com.fasterxml.jackson.databind.node.NullNode.instance
                    : response;
        } catch (RestClientResponseException exception) {
            throw rejected(providerCode, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw unavailable(providerCode);
        }
    }

    JsonNode post(URI uri, Object body, String fieldMask, String providerCode) {
        requireConfigured(providerCode);
        try {
            RestClient.RequestBodySpec request =
                    restClient
                            .post()
                            .uri(uri)
                            .header(API_KEY_HEADER, apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON);
            if (fieldMask != null && !fieldMask.isBlank()) {
                request = request.header(FIELD_MASK_HEADER, fieldMask);
            }
            JsonNode response = request.body(body).retrieve().body(JsonNode.class);
            return response == null
                    ? com.fasterxml.jackson.databind.node.NullNode.instance
                    : response;
        } catch (RestClientResponseException exception) {
            throw rejected(providerCode, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw unavailable(providerCode);
        }
    }

    private void requireConfigured(String providerCode) {
        if (!configured()) {
            throw EarthTripException.unavailable(
                    providerCode + "_NOT_CONFIGURED", "Google Maps Platform API 키가 설정되지 않았습니다.");
        }
    }

    private static EarthTripException rejected(String providerCode, int status) {
        return new EarthTripException(
                providerCode + "_REQUEST_REJECTED",
                status == 429 ? 503 : 502,
                "Google Maps Platform이 요청을 처리하지 못했습니다.",
                Map.of("providerStatus", status));
    }

    private static EarthTripException unavailable(String providerCode) {
        return EarthTripException.unavailable(
                providerCode + "_UNAVAILABLE", "Google Maps Platform에 연결할 수 없습니다.");
    }
}
