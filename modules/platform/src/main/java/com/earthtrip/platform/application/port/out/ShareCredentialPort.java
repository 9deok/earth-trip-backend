package com.earthtrip.platform.application.port.out;

public interface ShareCredentialPort {

    String newToken();

    String hashToken(String token);

    String encodePassword(String password);

    boolean matchesPassword(String password, String encodedPassword);
}
