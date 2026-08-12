package com.earthtrip.trip.adapter.out.serialization;

import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import com.earthtrip.trip.application.port.out.TripStructureSerializationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class JacksonTripStructureSerializationAdapter implements TripStructureSerializationPort {
    private final ObjectMapper objectMapper;

    JacksonTripStructureSerializationAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String proposalHash(TripStructureUseCase.StructureProposal proposal) {
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(proposal);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(serialized));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("구조 변경안 해시를 만들 수 없습니다.", exception);
        }
    }

    @Override
    public String serialize(TripStructureView.StructureSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("여행 구조 스냅샷을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public TripStructureView.StructureSnapshot deserialize(String value) {
        try {
            return objectMapper.readValue(value, TripStructureView.StructureSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 여행 구조 스냅샷을 읽을 수 없습니다.", exception);
        }
    }
}
