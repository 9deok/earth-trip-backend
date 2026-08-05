package com.earthtrip.notification.adapter.out.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.earthtrip.notification.application.port.out.PushDeliveryPort;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FirebasePushDeliveryAdapterTest {

    @Test
    void sendsNotificationAndDeepLinkThroughFcmHttpV1() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FirebasePushDeliveryAdapter adapter = new FirebasePushDeliveryAdapter(
            "earth-trip-project",
            builder.baseUrl("https://fcm.googleapis.com").build(),
            () -> "google-access-token"
        );
        server.expect(requestTo(
                "https://fcm.googleapis.com/v1/projects/earth-trip-project/messages:send"
            ))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer google-access-token"))
            .andExpect(jsonPath("$.message.token").value("device-token"))
            .andExpect(jsonPath("$.message.data.deep_link").value("earthtrip://today"))
            .andExpect(jsonPath("$.message.apns.payload.aps.sound").value("default"))
            .andRespond(withSuccess(
                "{\"name\":\"projects/earth-trip-project/messages/message-1\"}",
                MediaType.APPLICATION_JSON
            ));

        PushDeliveryPort.DeliveryResult result = adapter.send(
            "device-token",
            new PushDeliveryPort.PushMessage(
                "오늘 일정",
                "여행을 시작해요",
                "earthtrip://today",
                Map.of("trip_id", "trip-1")
            )
        );

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(result.providerMessageId()).endsWith("message-1");
        server.verify();
    }

    @Test
    void classifiesUnregisteredDeviceToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FirebasePushDeliveryAdapter adapter = new FirebasePushDeliveryAdapter(
            "earth-trip-project",
            builder.baseUrl("https://fcm.googleapis.com").build(),
            () -> "google-access-token"
        );
        server.expect(requestTo(
                "https://fcm.googleapis.com/v1/projects/earth-trip-project/messages:send"
            ))
            .andRespond(withResourceNotFound().body("UNREGISTERED"));

        PushDeliveryPort.DeliveryResult result = adapter.send(
            "expired-token",
            new PushDeliveryPort.PushMessage("제목", "본문", null, Map.of())
        );

        assertThat(result.invalidToken()).isTrue();
        server.verify();
    }
}
