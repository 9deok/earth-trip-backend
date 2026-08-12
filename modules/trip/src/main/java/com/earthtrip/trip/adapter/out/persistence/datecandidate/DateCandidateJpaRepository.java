package com.earthtrip.trip.adapter.out.persistence.datecandidate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DateCandidateJpaRepository extends JpaRepository<DateCandidateJpaEntity, String> {
    List<DateCandidateJpaEntity> findAllByTripIdOrderByStartDateAsc(String tripId);
}
