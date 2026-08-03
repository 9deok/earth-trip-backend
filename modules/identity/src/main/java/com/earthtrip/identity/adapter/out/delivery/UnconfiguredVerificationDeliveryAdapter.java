package com.earthtrip.identity.adapter.out.delivery;

import com.earthtrip.identity.application.port.out.VerificationDeliveryPort;
import com.earthtrip.identity.domain.EmailAddress;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredVerificationDeliveryAdapter implements VerificationDeliveryPort {

    @Override
    public DeliveryStatus sendEmailVerification(
        EmailAddress email,
        String rawToken,
        Instant expiresAt
    ) {
        return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
    }

    @Override
    public DeliveryStatus sendPasswordReset(
        EmailAddress email,
        String rawToken,
        Instant expiresAt
    ) {
        return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
    }

    @Override
    public DeliveryStatus sendEmailChange(
        EmailAddress email,
        String rawToken,
        Instant expiresAt
    ) {
        return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
    }
}
