package com.earthtrip.notification.adapter.out.security;

import com.earthtrip.notification.application.port.out.PushTokenProtectorPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class AesPushTokenProtector implements PushTokenProtectorPort {

    private static final int IV_LENGTH = 12;

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    AesPushTokenProtector(@Value("${earthtrip.push.token-encryption-key:}") String encoded) {
        this.key = decodeKey(encoded);
    }

    @Override
    public ProtectedToken protect(String token) {
        requireConfigured();
        if (token == null || token.isBlank()) {
            throw EarthTripException.badRequest("PUSH_TOKEN_REQUIRED", "푸시 토큰이 필요합니다.");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] joined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, joined, 0, iv.length);
            System.arraycopy(encrypted, 0, joined, iv.length, encrypted.length);
            return new ProtectedToken(hash(token), Base64.getEncoder().encodeToString(joined));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("푸시 토큰을 암호화할 수 없습니다.", exception);
        }
    }

    @Override
    public String reveal(String protectedToken) {
        requireConfigured();
        try {
            byte[] joined = Base64.getDecoder().decode(protectedToken);
            if (joined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("암호화된 푸시 토큰 형식이 올바르지 않습니다.");
            }
            byte[] iv = Arrays.copyOfRange(joined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(joined, IV_LENGTH, joined.length);
            return new String(
                    cipher(Cipher.DECRYPT_MODE, iv).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("푸시 토큰을 복호화할 수 없습니다.", exception);
        }
    }

    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher;
    }

    private void requireConfigured() {
        if (key.length != 32) {
            throw EarthTripException.unavailable(
                    "PUSH_TOKEN_ENCRYPTION_NOT_CONFIGURED", "푸시 토큰 암호화 키가 설정되지 않았습니다.");
        }
    }

    private static byte[] decodeKey(String encoded) {
        try {
            return encoded == null || encoded.isBlank()
                    ? new byte[0]
                    : Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
