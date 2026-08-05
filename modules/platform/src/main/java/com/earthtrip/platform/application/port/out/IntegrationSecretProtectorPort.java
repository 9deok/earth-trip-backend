package com.earthtrip.platform.application.port.out;

public interface IntegrationSecretProtectorPort {

    boolean configured();

    String protect(String purpose, String value);

    String reveal(String purpose, String protectedValue);
}
