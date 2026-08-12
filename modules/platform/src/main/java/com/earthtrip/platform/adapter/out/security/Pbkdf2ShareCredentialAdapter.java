package com.earthtrip.platform.adapter.out.security;

import com.earthtrip.platform.application.port.out.ShareCredentialPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
class Pbkdf2ShareCredentialAdapter implements ShareCredentialPort {

    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int PASSWORD_KEY_BITS = 256;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashToken(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    @Override
    public String encodePassword(String password) {
        byte[] salt = new byte[PASSWORD_SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PASSWORD_ITERATIONS);
        return "pbkdf2-sha256$"
                + PASSWORD_ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean matchesPassword(String password, String encodedPassword) {
        if (password == null) {
            return false;
        }
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) {
                throw corruptedPassword();
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalStateException) {
                throw exception;
            }
            throw corruptedPassword(exception);
        }
    }

    private static byte[] pbkdf2(char[] value, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(value, salt, iterations, PASSWORD_KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("공유 비밀번호를 안전하게 처리할 수 없습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static IllegalStateException corruptedPassword() {
        return new IllegalStateException("저장된 공유 비밀번호 해시가 올바르지 않습니다.");
    }

    private static IllegalStateException corruptedPassword(RuntimeException cause) {
        return new IllegalStateException("저장된 공유 비밀번호 해시가 올바르지 않습니다.", cause);
    }
}
