package com.earthtrip.identity.adapter.out.delivery;

import com.earthtrip.identity.application.port.out.InvitationDeliveryPort;
import com.earthtrip.identity.domain.EmailAddress;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredInvitationDeliveryAdapter implements InvitationDeliveryPort {
    @Override
    public DeliveryStatus send(EmailAddress email, String invitationUrl, Instant expiresAt) {
        return DeliveryStatus.PROVIDER_NOT_CONFIGURED;
    }
}
