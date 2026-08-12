package com.earthtrip.identity.adapter.out.delivery;

import com.earthtrip.identity.application.port.out.InvitationDeliveryPort;
import com.earthtrip.identity.domain.EmailAddress;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class SesInvitationDeliveryAdapter implements InvitationDeliveryPort {

    private final SesMailClient mail;
    private final String publicBaseUrl;

    SesInvitationDeliveryAdapter(
            SesMailClient mail,
            @Value("${earthtrip.public-base-url:https://app.earthtrip.local}")
                    String publicBaseUrl) {
        this.mail = mail;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public DeliveryStatus send(EmailAddress email, String invitationUrl, Instant expiresAt) {
        if (!mail.configured()) {
            return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
        }
        String safeUrl = HtmlUtils.htmlEscape(invitationUrl);
        try {
            mail.send(
                    email.value(),
                    "Earth Trip 여행 초대가 도착했습니다",
                    "아래 주소에서 여행 초대를 확인해 주세요.\n" + invitationUrl + "\n만료 시각: " + expiresAt,
                    EarthTripMailBrand.header(publicBaseUrl)
                            + "<p>Earth Trip 여행 초대가 도착했습니다.</p>"
                            + "<p><a href=\""
                            + safeUrl
                            + "\">여행 초대 확인하기</a></p>"
                            + "<p>만료 시각: "
                            + HtmlUtils.htmlEscape(expiresAt.toString())
                            + "</p>",
                    "trip-invitation");
            return DeliveryStatus.SENT;
        } catch (com.earthtrip.sharedkernel.error.EarthTripException exception) {
            return DeliveryStatus.FAILED;
        }
    }
}
