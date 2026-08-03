package com.earthtrip.wallet.adapter.out.persistence.reservationimport;

import com.earthtrip.wallet.application.port.out.ReservationImportStorePort;
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

    private final ReservationImportJobJpaRepository jobs;
    private final ReservationImportCandidateJpaRepository candidates;
    private final ObjectMapper json;

    ReservationImportPersistenceAdapter(
        ReservationImportJobJpaRepository jobs,
        ReservationImportCandidateJpaRepository candidates,
        ObjectMapper json
    ) {
        this.jobs = jobs;
        this.candidates = candidates;
        this.json = json;
    }

    @Override
    public Optional<JobRecord> findJob(UUID jobId) {
        return jobs.findById(jobId.toString()).map(this::job);
    }

    @Override
    public JobRecord saveJob(JobRecord job) {
        String payload = write(job.sourcePayload());
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
        String payload = write(candidate.payload());
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
        return entity.toRecord(read(entity.sourcePayload()));
    }

    private CandidateRecord candidate(ReservationImportCandidateJpaEntity entity) {
        return entity.toRecord(read(entity.payload()));
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
