package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.domain.DateCandidate;
import com.earthtrip.trip.domain.TripId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DateCandidateStorePort {
    List<DateCandidate> findAll(TripId tripId); Optional<DateCandidate> findById(UUID id);
    DateCandidate save(DateCandidate candidate); void delete(UUID id);
    List<AvailabilityRecord> availability(UUID id);
    AvailabilityRecord saveAvailability(UUID id, UUID userId, String availability, String note, Instant now);
    record AvailabilityRecord(UUID userId, String availability, String note, Instant updatedAt) { }
}
