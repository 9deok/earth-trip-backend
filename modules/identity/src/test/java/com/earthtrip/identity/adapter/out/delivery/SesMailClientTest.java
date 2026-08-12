package com.earthtrip.identity.adapter.out.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.sharedkernel.error.EarthTripException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

class SesMailClientTest {

    @Test
    void sendsUtf8TransactionalEmailThroughSesApi() {
        SesV2Client ses = mock(SesV2Client.class);
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("message-1").build());
        SesMailClient client =
                new SesMailClient(
                        ses, "Earth Trip <no-reply@earthtrip.test>", "earth-trip-transactional");

        client.send("traveler@example.com", "인증", "text", "<p>html</p>", "email-verification");

        ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses).sendEmail(request.capture());
        assertThat(request.getValue().fromEmailAddress())
                .isEqualTo("Earth Trip <no-reply@earthtrip.test>");
        assertThat(request.getValue().destination().toAddresses())
                .containsExactly("traveler@example.com");
        assertThat(request.getValue().content().simple().subject().data()).isEqualTo("인증");
        assertThat(request.getValue().content().simple().subject().charset()).isEqualTo("UTF-8");
        assertThat(request.getValue().content().simple().body().text().data()).isEqualTo("text");
        assertThat(request.getValue().content().simple().body().html().data())
                .isEqualTo("<p>html</p>");
        assertThat(request.getValue().configurationSetName()).isEqualTo("earth-trip-transactional");
        assertThat(request.getValue().emailTags())
                .singleElement()
                .satisfies(
                        tag -> {
                            assertThat(tag.name()).isEqualTo("earthtrip-purpose");
                            assertThat(tag.value()).isEqualTo("email-verification");
                        });
    }

    @Test
    void failsClosedWhenSesSenderIsMissing() {
        SesMailClient client = new SesMailClient(mock(SesV2Client.class), "", "");

        assertThatThrownBy(
                        () ->
                                client.send(
                                        "traveler@example.com",
                                        "인증",
                                        "text",
                                        "html",
                                        "verification"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error ->
                                assertThat(error.code())
                                        .isEqualTo("EMAIL_PROVIDER_NOT_CONFIGURED"));
    }

    @Test
    void mapsSesThrottlingToTemporaryProviderFailure() {
        SesV2Client ses = mock(SesV2Client.class);
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SesV2Exception.builder().statusCode(429).message("throttled").build());
        SesMailClient client = new SesMailClient(ses, "no-reply@earthtrip.test", "");

        assertThatThrownBy(
                        () ->
                                client.send(
                                        "traveler@example.com",
                                        "인증",
                                        "text",
                                        "html",
                                        "verification"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> {
                            assertThat(error.code()).isEqualTo("SES_PROVIDER_UNAVAILABLE");
                            assertThat(error.httpStatus()).isEqualTo(503);
                        });
    }
}
