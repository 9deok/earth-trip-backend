package com.earthtrip.platform.adapter.out.integration;

import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.platform.application.port.out.IntegrationSecretProtectorPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleCalendarProviderAdapter implements ExternalAccountProviderPort {

    private static final String REFRESH_TOKEN_PURPOSE = "google-calendar-refresh-token";
    private static final String REFRESH_TOKEN_KEY = "_earthTripGoogleRefreshToken";
    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.app.created";

    private final RestClient restClient;
    private final IntegrationSecretProtectorPort protector;
    private final String clientId;
    private final String clientSecret;
    private final String configuredRedirectUri;

    GoogleCalendarProviderAdapter(
            RestClient.Builder builder,
            IntegrationSecretProtectorPort protector,
            @Value("${earthtrip.providers.google-calendar.client-id:}") String clientId,
            @Value("${earthtrip.providers.google-calendar.client-secret:}") String clientSecret,
            @Value("${earthtrip.providers.google-calendar.callback-uri:}") String callbackUri) {
        this.restClient = builder.build();
        this.protector = protector;
        this.clientId = strip(clientId);
        this.clientSecret = strip(clientSecret);
        this.configuredRedirectUri = strip(callbackUri);
    }

    @Override
    public boolean supports(String provider) {
        return "GOOGLE".equals(provider) || "GOOGLE_CALENDAR".equals(provider);
    }

    @Override
    public boolean configured() {
        return !clientId.isBlank()
                && !clientSecret.isBlank()
                && !configuredRedirectUri.isBlank()
                && protector.configured();
    }

    @Override
    public AuthorizationResult authorize(AuthorizationCommand command) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", command.authorizationCode());
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri(command.redirectUri()));
        if (command.codeVerifier() != null && !command.codeVerifier().isBlank()) {
            form.add("code_verifier", command.codeVerifier().strip());
        }
        JsonNode token = token(form, "GOOGLE_CALENDAR_AUTHORIZATION_FAILED");
        String refreshToken = token.path("refresh_token").asText("");
        if (refreshToken.isBlank()) {
            throw EarthTripException.badRequest(
                    "GOOGLE_CALENDAR_REFRESH_TOKEN_REQUIRED",
                    "오프라인 접근 권한이 없어 Google Calendar 연결을 저장할 수 없습니다. 동의 화면에서 다시 승인해 주세요.");
        }
        Set<String> scopes = new LinkedHashSet<>();
        String granted = token.path("scope").asText("");
        boolean calendarGranted =
                java.util.Arrays.stream(granted.split("\\s+")).anyMatch(CALENDAR_SCOPE::equals);
        if (!calendarGranted) {
            throw EarthTripException.forbidden(
                    "GOOGLE_CALENDAR_SCOPE_REQUIRED", "Google Calendar 동기화 권한이 승인되지 않았습니다.");
        }
        scopes.add("CALENDAR_APP_CREATED");
        if (!granted.isBlank()) {
            for (String scope : granted.split("\\s+")) {
                scopes.add(scope);
            }
        }
        return new AuthorizationResult(
                Set.copyOf(scopes),
                Map.of(REFRESH_TOKEN_KEY, protector.protect(REFRESH_TOKEN_PURPOSE, refreshToken)));
    }

    @Override
    public void revoke(Map<String, Object> protectedMetadata) {
        String refreshToken = refreshToken(protectedMetadata);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        try {
            restClient
                    .post()
                    .uri("https://oauth2.googleapis.com/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ignored) {
            // 로컬 연결 해제를 막지 않는다. 만료된 토큰도 이미 해제된 것으로 취급한다.
        }
    }

    @Override
    public ConnectionCheckResult checkConnection(Map<String, Object> protectedMetadata) {
        requireConfigured();
        refreshAccessToken(refreshToken(protectedMetadata));
        return new ConnectionCheckResult(
                "ACTIVE", Map.of("provider", "GOOGLE_CALENDAR", "tokenRefresh", "SUCCEEDED"));
    }

    @Override
    public CalendarSyncResult syncCalendar(CalendarSyncCommand command) {
        requireConfigured();
        String accessToken = refreshAccessToken(refreshToken(command.protectedMetadata()));
        Map<String, Object> config = new LinkedHashMap<>(command.scopeConfig());
        String calendarId = string(config.get("calendarId"));
        if (calendarId.isBlank()) {
            calendarId =
                    createCalendar(accessToken, command.tripTitle(), command.defaultTimeZone());
            config.put("calendarId", calendarId);
            config.put("direction", "EARTH_TRIP_TO_GOOGLE");
        }

        Set<String> currentEventIds = new LinkedHashSet<>();
        int created = 0;
        int updated = 0;
        for (CalendarEvent event : command.events()) {
            String eventId = providerEventId(event.sourceId());
            currentEventIds.add(eventId);
            Map<String, Object> body = eventBody(command.tripId(), eventId, event);
            if (eventExists(accessToken, calendarId, eventId)) {
                updateEvent(accessToken, calendarId, eventId, body);
                updated++;
            } else {
                createEvent(accessToken, calendarId, body);
                created++;
            }
        }
        int deleted =
                deleteRemovedEvents(
                        accessToken, calendarId, command.tripId().toString(), currentEventIds);
        return new CalendarSyncResult(Map.copyOf(config), created, updated, deleted);
    }

    @Override
    public void deleteCalendar(CalendarDeleteCommand command) {
        requireConfigured();
        String calendarId = string(command.scopeConfig().get("calendarId"));
        if (calendarId.isBlank()) {
            return;
        }
        String accessToken = refreshAccessToken(refreshToken(command.protectedMetadata()));
        delete(calendarUri(calendarId), accessToken);
    }

    private String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        JsonNode token = token(form, "GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED");
        String accessToken = token.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw providerFailure("GOOGLE_CALENDAR_INVALID_TOKEN_RESPONSE", 502);
        }
        return accessToken;
    }

    private JsonNode token(MultiValueMap<String, String> form, String errorCode) {
        try {
            JsonNode response =
                    restClient
                            .post()
                            .uri("https://oauth2.googleapis.com/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null) {
                throw providerFailure("GOOGLE_CALENDAR_INVALID_TOKEN_RESPONSE", 502);
            }
            return response;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            int publicStatus = status == 400 || status == 401 ? 409 : 502;
            throw providerFailure(errorCode, publicStatus);
        } catch (RestClientException exception) {
            throw providerFailure("GOOGLE_CALENDAR_PROVIDER_UNAVAILABLE", 503);
        }
    }

    private String createCalendar(String accessToken, String title, String timeZone) {
        JsonNode response =
                post(
                        URI.create("https://www.googleapis.com/calendar/v3/calendars"),
                        accessToken,
                        Map.of("summary", "Earth Trip - " + title, "timeZone", timeZone));
        String id = response.path("id").asText("");
        if (id.isBlank()) {
            throw providerFailure("GOOGLE_CALENDAR_INVALID_RESPONSE", 502);
        }
        return id;
    }

    private boolean eventExists(String accessToken, String calendarId, String eventId) {
        try {
            get(eventUri(calendarId, eventId), accessToken);
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return false;
            }
            throw calendarRequestFailure(exception);
        }
    }

    private void createEvent(String accessToken, String calendarId, Map<String, Object> body) {
        post(eventsUri(calendarId), accessToken, body);
    }

    private void updateEvent(
            String accessToken, String calendarId, String eventId, Map<String, Object> body) {
        try {
            restClient
                    .put()
                    .uri(eventUri(calendarId, eventId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw calendarRequestFailure(exception);
        } catch (RestClientException exception) {
            throw providerFailure("GOOGLE_CALENDAR_PROVIDER_UNAVAILABLE", 503);
        }
    }

    private int deleteRemovedEvents(
            String accessToken, String calendarId, String tripId, Set<String> currentEventIds) {
        URI uri =
                UriComponentsBuilder.fromUriString("https://www.googleapis.com/calendar/v3")
                        .pathSegment("calendars", calendarId, "events")
                        .queryParam("privateExtendedProperty", "earthTripTripId=" + tripId)
                        .queryParam("maxResults", 2500)
                        .build()
                        .encode()
                        .toUri();
        JsonNode response;
        try {
            response = get(uri, accessToken);
        } catch (RestClientResponseException exception) {
            throw calendarRequestFailure(exception);
        }
        int deleted = 0;
        for (JsonNode item : response.path("items")) {
            String id = item.path("id").asText("");
            if (!id.isBlank() && !currentEventIds.contains(id)) {
                delete(eventUri(calendarId, id), accessToken);
                deleted++;
            }
        }
        return deleted;
    }

    private JsonNode post(URI uri, String accessToken, Object body) {
        try {
            JsonNode response =
                    restClient
                            .post()
                            .uri(uri)
                            .headers(headers -> headers.setBearerAuth(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
            return response == null
                    ? com.fasterxml.jackson.databind.node.NullNode.instance
                    : response;
        } catch (RestClientResponseException exception) {
            throw calendarRequestFailure(exception);
        } catch (RestClientException exception) {
            throw providerFailure("GOOGLE_CALENDAR_PROVIDER_UNAVAILABLE", 503);
        }
    }

    private JsonNode get(URI uri, String accessToken) {
        return restClient
                .get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(JsonNode.class);
    }

    private void delete(URI uri, String accessToken) {
        try {
            restClient
                    .delete()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw calendarRequestFailure(exception);
            }
        } catch (RestClientException exception) {
            throw providerFailure("GOOGLE_CALENDAR_PROVIDER_UNAVAILABLE", 503);
        }
    }

    private static Map<String, Object> eventBody(
            java.util.UUID tripId, String eventId, CalendarEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", eventId);
        body.put("summary", event.title());
        if (!event.description().isBlank()) {
            body.put("description", event.description());
        }
        if (!event.location().isBlank()) {
            body.put("location", event.location());
        }
        body.put(
                "extendedProperties",
                Map.of(
                        "private",
                        Map.of(
                                "earthTripTripId", tripId.toString(),
                                "earthTripSourceId", event.sourceId().toString())));
        if (!event.startDateTime().isBlank()) {
            body.put("start", dateTime(event.startDateTime(), event.timeZone()));
            String end =
                    event.endDateTime().isBlank() ? event.startDateTime() : event.endDateTime();
            body.put("end", dateTime(end, event.timeZone()));
        } else {
            LocalDate date = event.localDate();
            body.put("start", Map.of("date", date.toString()));
            body.put("end", Map.of("date", date.plusDays(1).toString()));
        }
        return Map.copyOf(body);
    }

    private static Map<String, String> dateTime(String value, String timeZone) {
        return Map.of("dateTime", value, "timeZone", timeZone);
    }

    private static URI eventsUri(String calendarId) {
        return UriComponentsBuilder.fromUriString("https://www.googleapis.com/calendar/v3")
                .pathSegment("calendars", calendarId, "events")
                .build()
                .encode()
                .toUri();
    }

    private static URI calendarUri(String calendarId) {
        return UriComponentsBuilder.fromUriString("https://www.googleapis.com/calendar/v3")
                .pathSegment("calendars", calendarId)
                .build()
                .encode()
                .toUri();
    }

    private static URI eventUri(String calendarId, String eventId) {
        return UriComponentsBuilder.fromUriString("https://www.googleapis.com/calendar/v3")
                .pathSegment("calendars", calendarId, "events", eventId)
                .build()
                .encode()
                .toUri();
    }

    private String refreshToken(Map<String, Object> metadata) {
        String protectedToken = string(metadata.get(REFRESH_TOKEN_KEY));
        if (protectedToken.isBlank()) {
            throw providerFailure("GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED", 409);
        }
        return protector.reveal(REFRESH_TOKEN_PURPOSE, protectedToken);
    }

    private String redirectUri(String requested) {
        String normalized = strip(requested);
        if (!configuredRedirectUri.isBlank() && !configuredRedirectUri.equals(normalized)) {
            throw EarthTripException.badRequest(
                    "GOOGLE_CALENDAR_REDIRECT_URI_MISMATCH",
                    "등록된 Google Calendar callback URI와 요청 값이 일치하지 않습니다.");
        }
        String result = configuredRedirectUri.isBlank() ? normalized : configuredRedirectUri;
        if (result.isBlank()) {
            throw EarthTripException.badRequest(
                    "GOOGLE_CALENDAR_REDIRECT_URI_REQUIRED",
                    "Google Calendar callback URI가 필요합니다.");
        }
        return result;
    }

    private void requireConfigured() {
        if (!configured()) {
            throw EarthTripException.unavailable(
                    "GOOGLE_CALENDAR_NOT_CONFIGURED", "Google Calendar OAuth 자격증명이 설정되지 않았습니다.");
        }
    }

    private static String providerEventId(java.util.UUID sourceId) {
        return "earthtrip" + sourceId.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static EarthTripException calendarRequestFailure(
            RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return providerFailure("GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED", 409);
        }
        return providerFailure("GOOGLE_CALENDAR_REQUEST_FAILED", status == 429 ? 503 : 502);
    }

    private static EarthTripException providerFailure(String code, int status) {
        return new EarthTripException(code, status, "Google Calendar 연동을 처리할 수 없습니다.");
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString().strip();
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }
}
