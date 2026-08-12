package com.earthtrip.wallet.adapter.out.serialization;

import com.earthtrip.wallet.application.port.out.ReservationProposalFingerprintPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JacksonReservationProposalFingerprintAdapter implements ReservationProposalFingerprintPort {

    private final ObjectMapper objectMapper;

    JacksonReservationProposalFingerprintAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String fingerprint(Map<String, Object> proposal) {
        try {
            byte[] encoded =
                    objectMapper
                            .writer()
                            .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                            .writeValueAsBytes(proposal);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("예약 변경안 해시를 계산할 수 없습니다.", exception);
        }
    }
}
