package com.earthtrip.notification.application.port.out;

import java.util.Map;

public interface PushDeliveryPort {

    DeliveryResult send(String rawDeviceToken, PushMessage message);

    record PushMessage(String title, String body, String deepLink, Map<String, String> data) {}

    record DeliveryResult(String status, String providerMessageId, String errorCode) {
        public boolean invalidToken() {
            return "INVALID_TOKEN".equals(status) || "UNREGISTERED".equals(status);
        }
    }
}
