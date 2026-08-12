package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.EmailAddress;
import java.time.Instant;

public interface VerificationDeliveryPort {

    DeliveryStatus sendEmailVerification(EmailAddress email, String rawToken, Instant expiresAt);

    DeliveryStatus sendPasswordReset(EmailAddress email, String rawToken, Instant expiresAt);

    DeliveryStatus sendEmailChange(EmailAddress email, String rawToken, Instant expiresAt);

    enum DeliveryStatus {
        SENT,
        PROVIDER_NOT_CONFIGURED
    }
}
