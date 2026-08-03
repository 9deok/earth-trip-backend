package com.earthtrip.identity.adapter.out.security;

import com.earthtrip.identity.application.port.out.CredentialPort;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
class JcaCredentialAdapter implements CredentialPort {

    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String hashPassword(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        String[] parts = encodedPassword.split("\\$", -1);
        if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 2_000_000) return false;
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt, iterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public String newToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    @Override
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("비밀번호를 안전하게 처리할 수 없습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
