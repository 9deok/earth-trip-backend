package com.earthtrip.identity.adapter.out.delivery;

import com.earthtrip.identity.application.port.out.VerificationDeliveryPort;
import com.earthtrip.identity.domain.EmailAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class SesVerificationDeliveryAdapter implements VerificationDeliveryPort {

    private final SesMailClient mail;
    private final String publicBaseUrl;

    SesVerificationDeliveryAdapter(
            SesMailClient mail,
            @Value("${earthtrip.public-base-url:https://app.earthtrip.local}")
                    String publicBaseUrl) {
        this.mail = mail;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public DeliveryStatus sendEmailVerification(
            EmailAddress email, String rawToken, Instant expiresAt) {
        return send(
                email,
                "Earth Trip 이메일을 인증해 주세요",
                "/auth/email-verification?token=" + encode(rawToken),
                expiresAt,
                "email-verification");
    }

    @Override
    public DeliveryStatus sendPasswordReset(
            EmailAddress email, String rawToken, Instant expiresAt) {
        return send(
                email,
                "Earth Trip 비밀번호 재설정",
                "/auth/password-reset?token=" + encode(rawToken),
                expiresAt,
                "password-reset");
    }

    @Override
    public DeliveryStatus sendEmailChange(EmailAddress email, String rawToken, Instant expiresAt) {
        return send(
                email,
                "Earth Trip 이메일 변경 확인",
                "/settings/account/email-change?token=" + encode(rawToken),
                expiresAt,
                "email-change");
    }

    private DeliveryStatus send(
            EmailAddress email, String subject, String path, Instant expiresAt, String purpose) {
        if (!mail.configured()) {
            return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
        }
        String url = publicBaseUrl + path;
        mail.send(
                email.value(),
                subject,
                subject + "\n" + url + "\n만료 시각: " + expiresAt,
                EarthTripMailBrand.header(publicBaseUrl)
                        + "<p>"
                        + HtmlUtils.htmlEscape(subject)
                        + "</p>"
                        + "<p><a href=\""
                        + HtmlUtils.htmlEscape(url)
                        + "\">계속하기</a></p>"
                        + "<p>만료 시각: "
                        + HtmlUtils.htmlEscape(expiresAt.toString())
                        + "</p>",
                purpose);
        return DeliveryStatus.SENT;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
