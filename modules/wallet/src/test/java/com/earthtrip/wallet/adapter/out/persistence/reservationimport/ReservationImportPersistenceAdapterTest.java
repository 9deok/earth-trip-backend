package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
import com.earthtrip.wallet.application.port.out.SensitiveWalletDataPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationImportPersistenceAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Test
    void 예약_가져오기_원문은_평문을_남기지_않고_round_trip한다() {
        ReservationImportJobJpaRepository jobs = mock(ReservationImportJobJpaRepository.class);
        ReservationImportCandidateJpaRepository candidates =
                mock(ReservationImportCandidateJpaRepository.class);
        when(jobs.findById(any())).thenReturn(Optional.empty());
        when(jobs.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReservationImportPersistenceAdapter adapter =
                new ReservationImportPersistenceAdapter(
                        jobs, candidates, new EncodedSensitiveData(), JSON);
        ReservationImportStorePort.JobRecord record =
                new ReservationImportStorePort.JobRecord(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "DOCUMENT_OCR",
                        Map.of("rawText", "예약번호 ABC-12345", "fileName", "hotel-confirmation.pdf"),
                        "READY",
                        null,
                        null,
                        1,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-04T00:00:00Z"),
                        Instant.parse("2026-08-04T00:00:00Z"),
                        0);

        ReservationImportStorePort.JobRecord saved = adapter.saveJob(record);

        assertThat(saved.sourcePayload()).isEqualTo(record.sourcePayload());
        org.mockito.ArgumentCaptor<ReservationImportJobJpaEntity> captor =
                org.mockito.ArgumentCaptor.forClass(ReservationImportJobJpaEntity.class);
        org.mockito.Mockito.verify(jobs).saveAndFlush(captor.capture());
        assertThat(captor.getValue().sourcePayload())
                .doesNotContain("ABC-12345", "hotel-confirmation.pdf")
                .contains("_earthTripEncryptedPayload");
    }

    private static final class EncodedSensitiveData implements SensitiveWalletDataPort {

        @Override
        public Object protect(String fieldName, Object value) {
            try {
                return Map.of(
                        "protected",
                        true,
                        "field",
                        fieldName,
                        "value",
                        Base64.getEncoder().encodeToString(JSON.writeValueAsBytes(value)));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public Object reveal(String fieldName, Object storedValue) {
            try {
                Map<?, ?> envelope = (Map<?, ?>) storedValue;
                assertThat(envelope.get("field")).isEqualTo(fieldName);
                byte[] bytes = Base64.getDecoder().decode(envelope.get("value").toString());
                return JSON.readValue(bytes, new TypeReference<Map<String, Object>>() {});
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public boolean isProtected(Object storedValue) {
            return storedValue instanceof Map<?, ?> map
                    && Boolean.TRUE.equals(map.get("protected"));
        }
    }
}
