package com.earthtrip.platform.adapter.out.security;

import com.earthtrip.platform.application.port.out.IntegrationSecretProtectorPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class AesIntegrationSecretProtector implements IntegrationSecretProtectorPort {

    private static final int IV_BYTES = 12;

    private final Map<String, byte[]> keys;
    private final String primaryKeyId;
    private final SecureRandom random = new SecureRandom();

    AesIntegrationSecretProtector(
        @Value("${earthtrip.integrations.encryption-keys:}") String encodedKeys,
        @Value("${earthtrip.integrations.primary-key-id:primary}") String primaryKeyId
    ) {
        this.keys = parseKeys(encodedKeys);
        this.primaryKeyId = primaryKeyId == null ? "" : primaryKeyId.strip();
    }

    @Override
    public boolean configured() {
        return keys.containsKey(primaryKeyId);
    }

    @Override
    public String protect(String purpose, String value) {
        byte[] key = keys.get(primaryKeyId);
        if (key == null) {
            throw EarthTripException.unavailable(
                "INTEGRATION_ENCRYPTION_NOT_CONFIGURED",
                "외부 계정 토큰 암호화 키가 설정되지 않았습니다."
            );
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, iv, purpose);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return String.join(
                ".",
                "v1",
                primaryKeyId,
                Base64.getUrlEncoder().withoutPadding().encodeToString(iv),
                Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("외부 계정 토큰을 암호화할 수 없습니다.", exception);
        }
    }

    @Override
    public String reveal(String purpose, String protectedValue) {
        try {
            String[] values = protectedValue.split("\\.", 4);
            if (values.length != 4 || !"v1".equals(values[0])) {
                throw corrupted();
            }
            byte[] key = keys.get(values[1]);
            if (key == null) {
                throw EarthTripException.unavailable(
                    "INTEGRATION_DECRYPTION_KEY_NOT_AVAILABLE",
                    "저장된 외부 계정 토큰을 복호화할 키가 없습니다."
                );
            }
            byte[] iv = Base64.getUrlDecoder().decode(values[2]);
            byte[] encrypted = Base64.getUrlDecoder().decode(values[3]);
            if (iv.length != IV_BYTES) {
                throw corrupted();
            }
            return new String(
                cipher(Cipher.DECRYPT_MODE, key, iv, purpose).doFinal(encrypted),
                StandardCharsets.UTF_8
            );
        } catch (AEADBadTagException exception) {
            throw corrupted();
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("외부 계정 토큰을 복호화할 수 없습니다.", exception);
        }
    }

    private static Cipher cipher(int mode, byte[] key, byte[] iv, String purpose)
        throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static Map<String, byte[]> parseKeys(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        Map<String, byte[]> parsed = new LinkedHashMap<>();
        for (String entry : value.split(",")) {
            String[] pair = entry.strip().split(":", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                continue;
            }
            try {
                byte[] key = Base64.getDecoder().decode(pair[1].strip());
                if (key.length == 32) {
                    parsed.put(pair[0].strip(), key);
                }
            } catch (IllegalArgumentException ignored) {
                // 잘못된 키를 제외해 암호화가 fail closed 되게 한다.
            }
        }
        return Map.copyOf(parsed);
    }

    private static EarthTripException corrupted() {
        return new EarthTripException(
            "INTEGRATION_SECRET_CORRUPTED",
            500,
            "저장된 외부 계정 토큰의 무결성을 확인할 수 없습니다."
        );
    }
}
