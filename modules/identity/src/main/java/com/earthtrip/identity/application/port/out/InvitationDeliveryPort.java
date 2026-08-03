package com.earthtrip.identity.application.port.out;

import com.earthtrip.identity.domain.EmailAddress;
import java.time.Instant;

public interface InvitationDeliveryPort {
    DeliveryStatus send(EmailAddress email, String invitationUrl, Instant expiresAt);
    enum DeliveryStatus { SENT, PROVIDER_NOT_CONFIGURED, FAILED }
}
