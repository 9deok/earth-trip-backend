package com.earthtrip.platform.adapter.out.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.platform.application.port.out.IntegrationSecretProtectorPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleCalendarProviderAdapterTest {

    private static final String CALENDAR_SCOPE =
            "https://www.googleapis.com/auth/calendar.app.created";

    @Test
    void exchangesAuthorizationCodeAndEncryptsRefreshToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleCalendarProviderAdapter adapter = adapter(builder);
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                "{\"access_token\":\"access\",\"refresh_token\":\"refresh\","
                                        + "\"scope\":\""
                                        + CALENDAR_SCOPE
                                        + "\"}",
                                MediaType.APPLICATION_JSON));

        var result =
                adapter.authorize(
                        new ExternalAccountProviderPort.AuthorizationCommand(
                                "authorization-code",
                                "https://app.earthtrip.test/oauth/calendar",
                                "verifier",
                                Set.of("CALENDAR_APP_CREATED")));

        assertThat(result.grantedScopes()).contains("CALENDAR_APP_CREATED", CALENDAR_SCOPE);
        assertThat(result.protectedMetadata().values()).contains("protected:refresh");
        server.verify();
    }

    @Test
    void createsDedicatedCalendarAndIdempotentEarthTripEvent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleCalendarProviderAdapter adapter = adapter(builder);
        UUID tripId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String eventId = "earthtrip" + itemId.toString().replace("-", "");

        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(
                        withSuccess("{\"access_token\":\"access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/calendar/v3/calendars"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"calendar-id\"}", MediaType.APPLICATION_JSON));
        server.expect(
                        requestTo(
                                "https://www.googleapis.com/calendar/v3/calendars/calendar-id/events/"
                                        + eventId))
                .andRespond(withResourceNotFound());
        server.expect(
                        requestTo(
                                "https://www.googleapis.com/calendar/v3/calendars/calendar-id/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(
                        jsonPath("$.extendedProperties.private.earthTripTripId")
                                .value(tripId.toString()))
                .andRespond(
                        withSuccess("{\"id\":\"" + eventId + "\"}", MediaType.APPLICATION_JSON));
        server.expect(
                        once(),
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "privateExtendedProperty=earthTripTripId%3D")))
                .andRespond(
                        withSuccess(
                                "{\"items\":[{\"id\":\"" + eventId + "\"}]}",
                                MediaType.APPLICATION_JSON));

        var result =
                adapter.syncCalendar(
                        new ExternalAccountProviderPort.CalendarSyncCommand(
                                tripId,
                                "서울 여행",
                                "Asia/Seoul",
                                Map.of("_earthTripGoogleRefreshToken", "protected:refresh"),
                                Map.of(),
                                List.of(
                                        new ExternalAccountProviderPort.CalendarEvent(
                                                itemId,
                                                LocalDate.parse("2026-08-03"),
                                                "경복궁",
                                                "오전 일정",
                                                "서울 종로구",
                                                "2026-08-03T10:00:00+09:00",
                                                "2026-08-03T11:00:00+09:00",
                                                "Asia/Seoul"))));

        assertThat(result.scopeConfig()).containsEntry("calendarId", "calendar-id");
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.deleted()).isZero();
        server.verify();
    }

    @Test
    void deletesOnlyTheDedicatedCalendarWhenExplicitlyRequested() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleCalendarProviderAdapter adapter = adapter(builder);
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess("{\"access_token\":\"access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/calendar/v3/calendars/calendar-id"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        adapter.deleteCalendar(
                new ExternalAccountProviderPort.CalendarDeleteCommand(
                        Map.of("_earthTripGoogleRefreshToken", "protected:refresh"),
                        Map.of("calendarId", "calendar-id")));

        server.verify();
    }

    private static GoogleCalendarProviderAdapter adapter(RestClient.Builder builder) {
        IntegrationSecretProtectorPort protector =
                new IntegrationSecretProtectorPort() {
                    @Override
                    public boolean configured() {
                        return true;
                    }

                    @Override
                    public String protect(String purpose, String value) {
                        return "protected:" + value;
                    }

                    @Override
                    public String reveal(String purpose, String protectedValue) {
                        return protectedValue.substring("protected:".length());
                    }
                };
        return new GoogleCalendarProviderAdapter(
                builder,
                protector,
                "calendar-client-id",
                "calendar-client-secret",
                "https://app.earthtrip.test/oauth/calendar");
    }
}
