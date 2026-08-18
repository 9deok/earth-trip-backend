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
            throw rejected(providerCode, exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw unavailable(providerCode, exception);
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
            throw rejected(providerCode, exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw unavailable(providerCode, exception);
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
            throw rejected(providerCode, exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw unavailable(providerCode, exception);
        }
    }

    BinaryResponse download(URI uri, String providerCode) {
        requireConfigured(providerCode);
        requireTrustedMediaUri(uri);
        try {
            return restClient
                    .get()
                    .uri(uri)
                    .accept(
                            MediaType.IMAGE_JPEG,
                            MediaType.IMAGE_PNG,
                            MediaType.parseMediaType("image/webp"))
                    .exchange(
                            (request, response) -> {
                                int status = response.getStatusCode().value();
                                if (!response.getStatusCode().is2xxSuccessful()) {
                                    throw rejected(providerCode, status, null);
                                }
                                byte[] bytes = response.getBody().readAllBytes();
                                MediaType mediaType = response.getHeaders().getContentType();
                                if (bytes.length == 0
                                        || bytes.length > 8 * 1024 * 1024
                                        || mediaType == null
                                        || !"image".equalsIgnoreCase(mediaType.getType())) {
                                    throw EarthTripException.unavailable(
                                            providerCode + "_INVALID_MEDIA",
                                            "Google Maps Platform 사진 응답이 올바르지 않습니다.");
                                }
                                return new BinaryResponse(bytes, mediaType.toString());
                            });
        } catch (RestClientResponseException exception) {
            throw rejected(providerCode, exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw unavailable(providerCode, exception);
        }
    }

    private void requireConfigured(String providerCode) {
        if (!configured()) {
            throw EarthTripException.unavailable(
                    providerCode + "_NOT_CONFIGURED", "Google Maps Platform API 키가 설정되지 않았습니다.");
        }
    }

    private static void requireTrustedMediaUri(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        boolean trustedHost =
                host.equals("googleusercontent.com")
                        || host.endsWith(".googleusercontent.com")
                        || host.equals("ggpht.com")
                        || host.endsWith(".ggpht.com");
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !trustedHost
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw EarthTripException.badRequest(
                    "UNTRUSTED_PLACE_PHOTO_URI", "허용되지 않은 장소 사진 주소입니다.");
        }
    }

    record BinaryResponse(byte[] bytes, String contentType) {}

    private static EarthTripException rejected(String providerCode, int status, Throwable cause) {
        return new EarthTripException(
                providerCode + "_REQUEST_REJECTED",
                status == 429 ? 503 : 502,
                "Google Maps Platform이 요청을 처리하지 못했습니다.",
                Map.of("providerStatus", status),
                cause);
    }

    private static EarthTripException unavailable(String providerCode, Throwable cause) {
        return EarthTripException.unavailable(
                providerCode + "_UNAVAILABLE", "Google Maps Platform에 연결할 수 없습니다.", cause);
    }
}
