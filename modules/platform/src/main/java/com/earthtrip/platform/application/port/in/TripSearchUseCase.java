package com.earthtrip.platform.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TripSearchUseCase {

    SearchResult search(
        UUID tripId,
        UUID actorUserId,
        String query,
        List<String> types,
        Integer limit
    );

    record SearchResult(String query, List<SearchItem> items, int totalCount) { }

    record SearchItem(
        UUID id,
        String domain,
        String type,
        String title,
        String subtitle,
        double score,
        Map<String, Object> highlights
    ) { }
}
