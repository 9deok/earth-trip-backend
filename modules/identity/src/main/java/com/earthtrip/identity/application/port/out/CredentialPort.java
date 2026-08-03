package com.earthtrip.identity.application.port.out;

public interface CredentialPort {

    String hashPassword(String rawPassword);

    boolean passwordMatches(String rawPassword, String encodedPassword);

    String newToken();

    String hashToken(String rawToken);
}
