package com.earthtrip.notification.application.port.out;public interface PushTokenProtectorPort{ProtectedToken protect(String rawToken);record ProtectedToken(String hash,String cipher){}}
