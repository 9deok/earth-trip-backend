package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
import com.earthtrip.wallet.application.port.out.SensitiveWalletDataPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReservationImportPersistenceAdapter implements ReservationImportStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final String PROTECTED_PAYLOAD = "_earthTripEncryptedPayload";
    private static final String JOB_PAYLOAD_FIELD = "reservationImportSourcePayload";
    private static final String CANDIDATE_PAYLOAD_FIELD = "reservationImportCandidatePayload";

    private final ReservationImportJobJpaRepository jobs;
    private final ReservationImportCandidateJpaRepository candidates;
    private final SensitiveWalletDataPort sensitiveData;
    private final ObjectMapper json;

    ReservationImportPersistenceAdapter(
        ReservationImportJobJpaRepository jobs,
        ReservationImportCandidateJpaRepository candidates,
        SensitiveWalletDataPort sensitiveData,
        ObjectMapper json
    ) {
        this.jobs = jobs;
        this.candidates = candidates;
        this.sensitiveData = sensitiveData;
        this.json = json;
    }

    @Override
    public Optional<JobRecord> findJob(UUID jobId) {
        return jobs.findById(jobId.toString()).map(this::job);
    }

    @Override
    public JobRecord saveJob(JobRecord job) {
        String payload = writeProtected(JOB_PAYLOAD_FIELD, job.sourcePayload());
        ReservationImportJobJpaEntity entity = jobs.findById(job.id().toString())
            .map(existing -> {
                existing.apply(job, payload);
                return existing;
            })
            .orElseGet(() -> new ReservationImportJobJpaEntity(job, payload));
        return job(jobs.saveAndFlush(entity));
    }

    @Override
    public List<CandidateRecord> findCandidates(UUID jobId) {
        return candidates.findAllByJobIdOrderByCreatedAtAsc(jobId.toString()).stream()
            .map(this::candidate)
            .toList();
    }

    @Override
    public Optional<CandidateRecord> findCandidate(UUID candidateId) {
        return candidates.findById(candidateId.toString()).map(this::candidate);
    }

    @Override
    public CandidateRecord saveCandidate(CandidateRecord candidate) {
        String payload = writeProtected(CANDIDATE_PAYLOAD_FIELD, candidate.payload());
        ReservationImportCandidateJpaEntity entity = candidates
            .findById(candidate.id().toString())
            .map(existing -> {
                existing.apply(candidate, payload);
                return existing;
            })
            .orElseGet(() -> new ReservationImportCandidateJpaEntity(candidate, payload));
        return candidate(candidates.saveAndFlush(entity));
    }

    private JobRecord job(ReservationImportJobJpaEntity entity) {
        return entity.toRecord(readProtected(JOB_PAYLOAD_FIELD, entity.sourcePayload()));
    }

    private CandidateRecord candidate(ReservationImportCandidateJpaEntity entity) {
        return entity.toRecord(readProtected(CANDIDATE_PAYLOAD_FIELD, entity.payload()));
    }

    private String writeProtected(String fieldName, Map<String, Object> value) {
        return write(Map.of(PROTECTED_PAYLOAD, sensitiveData.protect(fieldName, value)));
    }

    private Map<String, Object> readProtected(String fieldName, String value) {
        Map<String, Object> stored = read(value);
        Object protectedPayload = stored.get(PROTECTED_PAYLOAD);
        if (protectedPayload == null) {
            // 암호화 도입 이전 평문 레코드는 읽은 뒤 다음 저장에서 암호화한다.
            return stored;
        }
        Object revealed = sensitiveData.reveal(fieldName, protectedPayload);
        if (!(revealed instanceof Map<?, ?> map)) {
            throw new IllegalStateException("저장된 예약 가져오기 payload 형식이 올바르지 않습니다.");
        }
        Map<String, Object> normalized = new java.util.LinkedHashMap<>();
        map.forEach((key, nested) -> normalized.put(String.valueOf(key), nested));
        return Map.copyOf(normalized);
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("예약 가져오기 JSON을 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 예약 가져오기 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
