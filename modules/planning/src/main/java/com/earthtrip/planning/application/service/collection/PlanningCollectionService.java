package com.earthtrip.planning.application.service.collection;

import com.earthtrip.planning.application.port.in.PlanningCollectionUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class PlanningCollectionService implements PlanningCollectionUseCase {

    private final PlanningResourceUseCase resources;

    PlanningCollectionService(PlanningResourceUseCase resources) {
        this.resources = resources;
    }

    @Override
    public BatchResult createResearchSourceBatch(
        UUID tripId,
        UUID actorUserId,
        List<ResearchSourceItem> items
    ) {
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw EarthTripException.badRequest(
                "INVALID_RESEARCH_SOURCE_BATCH", "한 번에 1~100개의 자료를 저장할 수 있습니다."
            );
        }
        List<BatchItemResult> results = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ResearchSourceItem item = items.get(index);
            try {
                if (item == null || item.requestId() == null || item.payload() == null) {
                    throw EarthTripException.badRequest(
                        "INVALID_RESEARCH_SOURCE_ITEM", "자료 ID와 payload가 필요합니다."
                    );
                }
                PlanningResourceUseCase.ResourceResult created = resources.create(
                    tripId, actorUserId, "RESEARCH_SOURCE",
                    PlanningResourceUseCase.WritePermission.EDITOR,
                    new PlanningResourceUseCase.ResourceCommand(
                        item.requestId(), item.categoryId(), null, item.payload(), "ACTIVE",
                        item.sortOrder(), 0
                    )
                );
                results.add(new BatchItemResult(
                    index, item.requestId(), true, created, null, null
                ));
            } catch (EarthTripException exception) {
                results.add(new BatchItemResult(
                    index, item == null ? null : item.requestId(), false, null,
                    exception.code(), exception.getMessage()
                ));
            } catch (IllegalArgumentException exception) {
                results.add(new BatchItemResult(
                    index, item == null ? null : item.requestId(), false, null,
                    "INVALID_RESEARCH_SOURCE_ITEM", exception.getMessage()
                ));
            }
        }
        int succeeded = (int) results.stream().filter(BatchItemResult::succeeded).count();
        return new BatchResult(
            results.size(), succeeded, results.size() - succeeded, List.copyOf(results)
        );
    }

    @Override
    public List<DuplicateResult> researchSourceDuplicates(
        UUID tripId,
        UUID actorUserId,
        DuplicateQuery query
    ) {
        return duplicates(tripId, actorUserId, "RESEARCH_SOURCE", query, true);
    }

    @Override
    public List<DuplicateResult> placeCandidateDuplicates(
        UUID tripId,
        UUID actorUserId,
        DuplicateQuery query
    ) {
        return duplicates(tripId, actorUserId, "PLACE_CANDIDATE", query, false);
    }

    private List<DuplicateResult> duplicates(
        UUID tripId,
        UUID actorUserId,
        String type,
        DuplicateQuery query,
        boolean research
    ) {
        if (query == null || (query.anchorId() == null && query.payload() == null)) {
            throw EarthTripException.badRequest(
                "DUPLICATE_QUERY_INPUT_REQUIRED", "기준 항목 ID 또는 비교할 payload가 필요합니다."
            );
        }
        double minimum = query.minimumScore() == null ? 0.5 : query.minimumScore();
        if (minimum < 0 || minimum > 1) {
            throw EarthTripException.badRequest(
                "INVALID_DUPLICATE_SCORE", "최소 중복 점수는 0에서 1 사이여야 합니다."
            );
        }
        Map<String, Object> anchor = query.anchorId() == null
            ? copy(query.payload())
            : resources.get(tripId, actorUserId, type, query.anchorId()).payload();
        return resources.list(tripId, actorUserId, type, null, null).stream()
            .filter(resource -> !resource.resourceId().equals(query.anchorId()))
            .map(resource -> research
                ? researchDuplicate(resource, anchor)
                : placeDuplicate(resource, anchor))
            .filter(result -> result.score() >= minimum)
            .sorted((left, right) -> Double.compare(right.score(), left.score()))
            .toList();
    }

    private static DuplicateResult researchDuplicate(
        PlanningResourceUseCase.ResourceResult resource,
        Map<String, Object> anchor
    ) {
        List<String> reasons = new ArrayList<>();
        double score = 0;
        if (same(anchor, resource.payload(), List.of("canonicalUrl", "url", "sourceUrl"))) {
            score += 0.65;
            reasons.add("SAME_URL");
        }
        if (same(anchor, resource.payload(), List.of("fileChecksumSha256", "checksumSha256"))) {
            score += 0.9;
            reasons.add("SAME_FILE_CHECKSUM");
        }
        if (same(anchor, resource.payload(), List.of("title", "name"))) {
            score += 0.25;
            reasons.add("SAME_TITLE");
        }
        if (same(anchor, resource.payload(), List.of("author", "channelName", "creator"))) {
            score += 0.1;
            reasons.add("SAME_AUTHOR");
        }
        return new DuplicateResult(
            resource.resourceId(), Math.min(1, score), List.copyOf(reasons), resource
        );
    }

    private static DuplicateResult placeDuplicate(
        PlanningResourceUseCase.ResourceResult resource,
        Map<String, Object> anchor
    ) {
        List<String> reasons = new ArrayList<>();
        double score = 0;
        if (same(anchor, resource.payload(), List.of("providerPlaceId", "placeId", "googlePlaceId"))) {
            score += 0.9;
            reasons.add("SAME_PROVIDER_PLACE_ID");
        }
        Double distance = distanceMeters(anchor, resource.payload());
        if (distance != null && distance <= 100) {
            score += distance <= 25 ? 0.65 : 0.5;
            reasons.add("NEARBY_COORDINATES");
        }
        if (same(anchor, resource.payload(), List.of("name", "title"))) {
            score += 0.35;
            reasons.add("SAME_NAME");
        }
        return new DuplicateResult(
            resource.resourceId(), Math.min(1, score), List.copyOf(reasons), resource
        );
    }

    private static boolean same(
        Map<String, Object> left,
        Map<String, Object> right,
        List<String> keys
    ) {
        String leftValue = firstText(left, keys);
        String rightValue = firstText(right, keys);
        return leftValue != null && leftValue.equals(rightValue);
    }

    private static String firstText(Map<String, Object> value, List<String> keys) {
        for (String key : keys) {
            Object raw = value.get(key);
            if (raw != null && !String.valueOf(raw).isBlank()) {
                return normalize(String.valueOf(raw));
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Double distanceMeters(
        Map<String, Object> left,
        Map<String, Object> right
    ) {
        Double lat1 = number(left, "latitude", "lat");
        Double lon1 = number(left, "longitude", "lng", "lon");
        Double lat2 = number(right, "latitude", "lat");
        Double lon2 = number(right, "longitude", "lng", "lon");
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        double latRadians = Math.toRadians(lat2 - lat1);
        double lonRadians = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latRadians / 2) * Math.sin(latRadians / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonRadians / 2) * Math.sin(lonRadians / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Double number(Map<String, Object> value, String... keys) {
        for (String key : keys) {
            Object raw = value.get(key);
            if (raw instanceof Number number) {
                return number.doubleValue();
            }
            if (raw != null) {
                try {
                    return Double.parseDouble(String.valueOf(raw));
                } catch (NumberFormatException ignored) {
                    // Try the next accepted field name.
                }
            }
        }
        return null;
    }

    private static Map<String, Object> copy(Map<String, Object> payload) {
        return payload == null ? Map.of() : new LinkedHashMap<>(payload);
    }
}
