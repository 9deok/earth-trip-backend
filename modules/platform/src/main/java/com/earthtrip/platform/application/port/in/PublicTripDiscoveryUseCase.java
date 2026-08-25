package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PublicTripDiscoveryUseCase {

    List<PublicTripSummary> discover(String destination, int limit);

    record PublicTripSummary(
            UUID publicationId,
            String title,
            String authorName,
            String publicNote,
            LocalDate startDate,
            LocalDate endDate,
            List<String> cities,
            int itineraryCount,
            long viewCount,
            long likeCount,
            long helpfulCount,
            long commentCount,
            long copyCount,
            String heroPhotoUrl,
            Instant publishedAt) {}
}
