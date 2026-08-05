package com.earthtrip.notification.adapter.out.push;

import com.earthtrip.notification.application.port.out.PushDeliveryPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
class FirebasePushDeliveryAdapter implements PushDeliveryPort {

    private static final String FIREBASE_SCOPE =
        "https://www.googleapis.com/auth/firebase.messaging";

    private final String projectId;
    private final RestClient restClient;
    private final AccessTokenSupplier accessTokenSupplier;

    @Autowired
    FirebasePushDeliveryAdapter(
        @Value("${earthtrip.providers.firebase.project-id:}") String projectId,
        RestClient.Builder restClientBuilder
    ) {
        this(
            projectId,
            restClientBuilder.baseUrl("https://fcm.googleapis.com").build(),
            new GoogleAccessTokenSupplier()
        );
    }

    FirebasePushDeliveryAdapter(
        String projectId,
        RestClient restClient,
        AccessTokenSupplier accessTokenSupplier
    ) {
        this.projectId = projectId == null ? "" : projectId.strip();
        this.restClient = restClient;
        this.accessTokenSupplier = accessTokenSupplier;
    }

    @Override
    public DeliveryResult send(String rawDeviceToken, PushMessage message) {
        requireConfigured();
        try {
            String bearerToken = accessTokenSupplier.accessToken();
            JsonNode response = restClient.post()
                .uri("/v1/projects/{projectId}/messages:send", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .body(Map.of("message", providerMessage(rawDeviceToken, message)))
                .retrieve()
                .body(JsonNode.class);
            String providerMessageId = response == null ? null : response.path("name").asText(null);
            return new DeliveryResult("DELIVERED", providerMessageId, null);
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            if (body.contains("UNREGISTERED") || body.contains("registration-token-not-registered")) {
                return new DeliveryResult("UNREGISTERED", null, "UNREGISTERED");
            }
            int status = exception.getStatusCode().value();
            if (status == 404 || body.contains("INVALID_ARGUMENT")) {
                return new DeliveryResult("INVALID_TOKEN", null, "INVALID_ARGUMENT");
            }
            if (status == 408 || status == 429 || status >= 500) {
                return new DeliveryResult("TEMPORARY_FAILURE", null, "FCM_HTTP_" + status);
            }
            return new DeliveryResult("FAILED", null, "FCM_HTTP_" + status);
        } catch (IOException exception) {
            return new DeliveryResult("TEMPORARY_FAILURE", null, "FCM_AUTH_UNAVAILABLE");
        } catch (RuntimeException exception) {
            return new DeliveryResult("TEMPORARY_FAILURE", null, "FCM_TRANSPORT_FAILURE");
        }
    }

    private Map<String, Object> providerMessage(String token, PushMessage message) {
        Map<String, String> data = new LinkedHashMap<>(message.data());
        if (message.deepLink() != null && !message.deepLink().isBlank()) {
            data.put("deep_link", message.deepLink());
        }
        Map<String, Object> providerMessage = new LinkedHashMap<>();
        providerMessage.put("token", token);
        providerMessage.put("notification", Map.of(
            "title", message.title(),
            "body", message.body()
        ));
        providerMessage.put("data", data);
        providerMessage.put("android", Map.of(
            "priority", "high",
            "notification", Map.of("sound", "default")
        ));
        providerMessage.put("apns", Map.of(
            "payload", Map.of("aps", Map.of("sound", "default"))
        ));
        return providerMessage;
    }

    private void requireConfigured() {
        if (projectId.isBlank()) {
            throw EarthTripException.unavailable(
                "FIREBASE_NOT_CONFIGURED",
                "Firebase 프로젝트 ID가 설정되지 않았습니다."
            );
        }
    }

    @FunctionalInterface
    interface AccessTokenSupplier {
        String accessToken() throws IOException;
    }

    private static final class GoogleAccessTokenSupplier implements AccessTokenSupplier {

        private GoogleCredentials credentials;

        @Override
        public synchronized String accessToken() throws IOException {
            if (credentials == null) {
                credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of(FIREBASE_SCOPE));
            }
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() == null) {
                credentials.refresh();
            }
            return credentials.getAccessToken().getTokenValue();
        }
    }
}
