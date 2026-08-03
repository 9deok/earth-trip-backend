package com.earthtrip.notification.api;

public interface PushDeliveryEvents {

    void recordDelivery(String deviceId, String status, String providerMessageId);
}
