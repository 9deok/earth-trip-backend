package com.earthtrip.identity.adapter.out.delivery;

import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.MessageTag;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Component
class SesMailClient {

    private static final String DEFAULT_REGION = "ap-northeast-2";

    private final SesV2Client ses;
    private final String fromEmail;
    private final String configurationSet;

    @Autowired
    SesMailClient(
            @Value("${earthtrip.providers.ses.region:ap-northeast-2}") String region,
            @Value("${earthtrip.providers.ses.from-email:}") String fromEmail,
            @Value("${earthtrip.providers.ses.configuration-set:}") String configurationSet) {
        this(
                SesV2Client.builder().region(Region.of(region(region))).build(),
                fromEmail,
                configurationSet);
    }

    SesMailClient(SesV2Client ses, String fromEmail, String configurationSet) {
        this.ses = ses;
        this.fromEmail = normalize(fromEmail);
        this.configurationSet = normalize(configurationSet);
    }

    boolean configured() {
        return !fromEmail.isBlank();
    }

    void send(String to, String subject, String textBody, String htmlBody, String purpose) {
        requireConfigured();
        SendEmailRequest.Builder request =
                SendEmailRequest.builder()
                        .fromEmailAddress(fromEmail)
                        .destination(Destination.builder().toAddresses(to).build())
                        .content(
                                EmailContent.builder()
                                        .simple(
                                                Message.builder()
                                                        .subject(content(subject))
                                                        .body(
                                                                Body.builder()
                                                                        .text(content(textBody))
                                                                        .html(content(htmlBody))
                                                                        .build())
                                                        .build())
                                        .build())
                        .emailTags(
                                List.of(
                                        MessageTag.builder()
                                                .name("earthtrip-purpose")
                                                .value(tagValue(purpose))
                                                .build()));
        if (!configurationSet.isBlank()) {
            request.configurationSetName(configurationSet);
        }

        try {
            SendEmailResponse response = ses.sendEmail(request.build());
            if (response == null || normalize(response.messageId()).isBlank()) {
                throw rejected(-1, "EMPTY_MESSAGE_ID");
            }
        } catch (SesV2Exception exception) {
            int status = exception.statusCode();
            String providerCode =
                    exception.awsErrorDetails() == null
                            ? exception.getClass().getSimpleName()
                            : normalize(exception.awsErrorDetails().errorCode());
            if (status == 429 || status >= 500) {
                throw new EarthTripException(
                        "SES_PROVIDER_UNAVAILABLE",
                        503,
                        "AWS SES 메일 제공자가 일시적으로 요청을 처리할 수 없습니다.",
                        Map.of("providerStatus", status, "providerCode", providerCode));
            }
            throw rejected(status, providerCode);
        } catch (SdkClientException exception) {
            throw EarthTripException.unavailable(
                    "SES_PROVIDER_UNAVAILABLE", "AWS SES 메일 제공자에 연결할 수 없습니다.");
        }
    }

    private EarthTripException rejected(int status, String providerCode) {
        return new EarthTripException(
                "SES_DELIVERY_REJECTED",
                502,
                "AWS SES가 메일 발송 요청을 거절했습니다.",
                Map.of("providerStatus", status, "providerCode", providerCode));
    }

    private void requireConfigured() {
        if (!configured()) {
            throw EarthTripException.unavailable(
                    "EMAIL_PROVIDER_NOT_CONFIGURED", "AWS SES 발신 주소가 설정되지 않았습니다.");
        }
    }

    private static Content content(String value) {
        return Content.builder().data(value).charset("UTF-8").build();
    }

    private static String tagValue(String value) {
        String normalized = normalize(value).replaceAll("[^A-Za-z0-9_-]", "-");
        return normalized.isBlank() ? "transactional" : normalized;
    }

    private static String region(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? DEFAULT_REGION : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
