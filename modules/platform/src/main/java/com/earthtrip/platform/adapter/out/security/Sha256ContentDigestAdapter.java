package com.earthtrip.platform.adapter.out.security;

import com.earthtrip.platform.application.port.out.ContentDigestPort;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class Sha256ContentDigestAdapter implements ContentDigestPort {

    @Override
    public String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
