package com.earthtrip.notification.application.port.out;

public interface PushTokenProtectorPort {

    ProtectedToken protect(String rawToken);

    String reveal(String protectedToken);

    record ProtectedToken(String hash, String cipher) { }
}
