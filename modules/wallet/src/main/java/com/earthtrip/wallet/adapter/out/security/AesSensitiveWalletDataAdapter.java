package com.earthtrip.wallet.adapter.out.security;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.wallet.application.port.out.SensitiveWalletDataPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AesSensitiveWalletDataAdapter implements SensitiveWalletDataPort {

    private static final String MARKER = "_earthTripProtected";
    private static final String ALGORITHM = "AES-256-GCM";
    private static final int IV_BYTES = 12;

    private final Map<String, byte[]> keys;
    private final String primaryKeyId;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();

    AesSensitiveWalletDataAdapter(
            @Value("${earthtrip.wallet.encryption-keys:}") String encodedKeys,
            @Value("${earthtrip.wallet.primary-key-id:primary}") String primaryKeyId,
            ObjectMapper json) {
        this.keys = parseKeys(encodedKeys);
        this.primaryKeyId = primaryKeyId == null ? "" : primaryKeyId.strip();
        this.json = json;
    }

    @Override
    public Object protect(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        if (isProtected(value)) {
            throw EarthTripException.badRequest(
                    "SENSITIVE_VALUE_ALREADY_PROTECTED", "암호화 envelope를 API 값으로 직접 저장할 수 없습니다.");
        }
        byte[] key = keys.get(primaryKeyId);
        if (key == null) {
            throw EarthTripException.unavailable(
                    "WALLET_ENCRYPTION_NOT_CONFIGURED", "민감 예약정보 암호화 키가 설정되지 않았습니다.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, iv));
            cipher.updateAAD(fieldName.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(json.writeValueAsBytes(value));
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put(MARKER, true);
            envelope.put("algorithm", ALGORITHM);
            envelope.put("keyId", primaryKeyId);
            envelope.put("iv", Base64.getEncoder().encodeToString(iv));
            envelope.put("ciphertext", Base64.getEncoder().encodeToString(encrypted));
            return Map.copyOf(envelope);
        } catch (GeneralSecurityException | JsonProcessingException exception) {
            throw new IllegalStateException("민감 예약정보를 암호화할 수 없습니다.", exception);
        }
    }

    @Override
    public Object reveal(String fieldName, Object storedValue) {
        if (!isProtected(storedValue)) {
            return storedValue;
        }
        Map<?, ?> envelope = (Map<?, ?>) storedValue;
        String keyId = string(envelope, "keyId");
        byte[] key = keys.get(keyId);
        if (key == null) {
            throw EarthTripException.unavailable(
                    "WALLET_DECRYPTION_KEY_NOT_AVAILABLE", "저장된 민감 예약정보를 복호화할 키가 없습니다.");
        }
        if (!ALGORITHM.equals(string(envelope, "algorithm"))) {
            throw corrupted();
        }
        try {
            byte[] iv = Base64.getDecoder().decode(string(envelope, "iv"));
            byte[] encrypted = Base64.getDecoder().decode(string(envelope, "ciphertext"));
            if (iv.length != IV_BYTES) {
                throw corrupted();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, iv));
            cipher.updateAAD(fieldName.getBytes(StandardCharsets.UTF_8));
            return json.readValue(cipher.doFinal(encrypted), Object.class);
        } catch (AEADBadTagException exception) {
            throw corrupted();
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            throw new IllegalStateException("민감 예약정보를 복호화할 수 없습니다.", e);
        }
    }

    @Override
    public boolean isProtected(Object storedValue) {
        return storedValue instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get(MARKER));
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
                // 잘못된 키는 사용 가능한 key ring에 넣지 않아 민감값 저장을 fail closed 한다.
            }
        }
        return Map.copyOf(parsed);
    }

    private static String string(Map<?, ?> envelope, String key) {
        Object value = envelope.get(key);
        if (value == null || value.toString().isBlank()) {
            throw corrupted();
        }
        return value.toString();
    }

    private static EarthTripException corrupted() {
        return new EarthTripException(
                "SENSITIVE_WALLET_DATA_CORRUPTED", 500, "저장된 민감 예약정보의 무결성을 확인할 수 없습니다.");
    }
}
