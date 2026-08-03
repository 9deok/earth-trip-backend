package com.earthtrip.planning.application.service.merge;

import com.earthtrip.planning.application.port.in.PlanningMergeUseCase;
import com.earthtrip.planning.application.port.in.PlanningResourceUseCase;
import com.earthtrip.planning.application.port.out.CandidateSourceLinkStorePort;
import com.earthtrip.planning.application.port.out.PlanningMergeStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PlanningMergeService implements PlanningMergeUseCase {

    private final TripAccess access;
    private final PlanningResourceUseCase resources;
    private final PlanningMergeStorePort merges;
    private final CandidateSourceLinkStorePort links;
    private final Clock clock;

    PlanningMergeService(
        TripAccess access,
        PlanningResourceUseCase resources,
        PlanningMergeStorePort merges,
        CandidateSourceLinkStorePort links,
        Clock clock
    ) {
        this.access = access;
        this.resources = resources;
        this.merges = merges;
        this.links = links;
        this.clock = clock;
    }

    @Override
    public MergeResult mergeResearchSources(
        UUID tripId,
        UUID actorUserId,
        MergeCommand command
    ) {
        return merge(tripId, actorUserId, "RESEARCH_SOURCE", command);
    }

    @Override
    public MergeResult mergePlaceCandidates(
        UUID tripId,
        UUID actorUserId,
        MergeCommand command
    ) {
        return merge(tripId, actorUserId, "PLACE_CANDIDATE", command);
    }

    @Override
    public MergeResult revertResearchSourceMerge(
        UUID tripId,
        UUID mergeId,
        UUID actorUserId
    ) {
        return revert(tripId, mergeId, actorUserId, "RESEARCH_SOURCE");
    }

    @Override
    public MergeResult revertPlaceCandidateMerge(
        UUID tripId,
        UUID mergeId,
        UUID actorUserId
    ) {
        return revert(tripId, mergeId, actorUserId, "PLACE_CANDIDATE");
    }

    private MergeResult merge(
        UUID tripId,
        UUID actorUserId,
        String type,
        MergeCommand command
    ) {
        access.requireEditor(tripId, actorUserId);
        validate(command);
        PlanningMergeStorePort.MergeRecord existing = merges.find(command.requestId())
            .orElse(null);
        if (existing != null) {
            requireScope(existing, tripId, type);
            return currentResult(existing, actorUserId);
        }
        PlanningResourceUseCase.ResourceResult primary = resources.get(
            tripId, actorUserId, type, command.primaryId()
        );
        requireVersion(primary.version(), command.primaryBaseVersion());
        List<PlanningResourceUseCase.ResourceResult> duplicates = new ArrayList<>();
        for (UUID duplicateId : command.duplicateIds()) {
            PlanningResourceUseCase.ResourceResult duplicate = resources.get(
                tripId, actorUserId, type, duplicateId
            );
            Long expected = command.duplicateBaseVersions().get(duplicateId);
            if (expected == null) {
                throw EarthTripException.badRequest(
                    "MERGE_BASE_VERSION_REQUIRED", "모든 중복 항목의 기준 버전이 필요합니다."
                );
            }
            requireVersion(duplicate.version(), expected);
            duplicates.add(duplicate);
        }
        Map<String, Object> before = snapshot(primary, duplicates);
        PlanningResourceUseCase.ResourceResult mergedPrimary = resources.update(
            tripId, actorUserId, type, primary.resourceId(),
            PlanningResourceUseCase.WritePermission.EDITOR,
            new PlanningResourceUseCase.ResourceCommand(
                primary.resourceId(), null, null, mergedPayload(primary, command),
                primary.status(), primary.sortOrder(), primary.version()
            )
        );
        List<PlanningResourceUseCase.ResourceResult> mergedDuplicates = new ArrayList<>();
        for (PlanningResourceUseCase.ResourceResult duplicate : duplicates) {
            Map<String, Object> payload = new LinkedHashMap<>(duplicate.payload());
            payload.put("mergedIntoId", primary.resourceId().toString());
            mergedDuplicates.add(resources.update(
                tripId, actorUserId, type, duplicate.resourceId(),
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                    duplicate.resourceId(), null, null, Map.copyOf(payload), "MERGED",
                    duplicate.sortOrder(), duplicate.version()
                )
            ));
        }
        List<Map<String, Object>> addedLinks = preserveLinks(
            tripId, type, primary.resourceId(), command.duplicateIds(), actorUserId
        );
        Instant now = clock.instant();
        PlanningMergeStorePort.MergeRecord saved = merges.save(
            new PlanningMergeStorePort.MergeRecord(
                command.requestId(), tripId, type, primary.resourceId(),
                List.copyOf(command.duplicateIds()), before,
                snapshot(mergedPrimary, mergedDuplicates), addedLinks, "APPLIED",
                actorUserId, now, null, null, 0
            )
        );
        return result(saved, mergedPrimary, mergedDuplicates);
    }

    private MergeResult revert(
        UUID tripId,
        UUID mergeId,
        UUID actorUserId,
        String type
    ) {
        access.requireEditor(tripId, actorUserId);
        PlanningMergeStorePort.MergeRecord merge = merges.find(mergeId)
            .orElseThrow(() -> EarthTripException.notFound(
                "PLANNING_MERGE_NOT_FOUND", "병합 기록을 찾을 수 없습니다."
            ));
        requireScope(merge, tripId, type);
        if (merge.status().equals("REVERTED")) {
            return currentResult(merge, actorUserId);
        }
        List<PlanningResourceUseCase.ResourceResult> current = currentResources(
            merge, actorUserId
        );
        for (PlanningResourceUseCase.ResourceResult resource : current) {
            long expected = versionFrom(merge.afterSnapshot(), resource.resourceId());
            requireVersion(resource.version(), expected);
        }
        List<PlanningResourceUseCase.ResourceResult> restored = new ArrayList<>();
        for (PlanningResourceUseCase.ResourceResult resource : current) {
            Map<String, Object> before = resourceFrom(
                merge.beforeSnapshot(), resource.resourceId()
            );
            restored.add(resources.update(
                tripId, actorUserId, type, resource.resourceId(),
                PlanningResourceUseCase.WritePermission.EDITOR,
                new PlanningResourceUseCase.ResourceCommand(
                    resource.resourceId(), null, null, payloadFrom(before),
                    text(before, "status"), number(before, "sortOrder").intValue(),
                    resource.version()
                )
            ));
        }
        merge.addedLinks().forEach(link -> links.delete(
            UUID.fromString(String.valueOf(link.get("candidateId"))),
            UUID.fromString(String.valueOf(link.get("sourceId")))
        ));
        PlanningMergeStorePort.MergeRecord reverted = merges.save(
            new PlanningMergeStorePort.MergeRecord(
                merge.id(), merge.tripId(), merge.resourceType(), merge.primaryId(),
                merge.duplicateIds(), merge.beforeSnapshot(), merge.afterSnapshot(),
                merge.addedLinks(), "REVERTED", merge.mergedBy(), merge.mergedAt(),
                actorUserId, clock.instant(), merge.version()
            )
        );
        return result(reverted, restored.getFirst(), restored.subList(1, restored.size()));
    }

    private List<Map<String, Object>> preserveLinks(
        UUID tripId,
        String type,
        UUID primaryId,
        List<UUID> duplicateIds,
        UUID actorUserId
    ) {
        List<Map<String, Object>> added = new ArrayList<>();
        for (UUID duplicateId : duplicateIds) {
            List<CandidateSourceLinkStorePort.LinkRecord> duplicateLinks = type.equals("PLACE_CANDIDATE")
                ? links.findByCandidateId(duplicateId)
                : links.findBySourceId(duplicateId);
            for (CandidateSourceLinkStorePort.LinkRecord link : duplicateLinks) {
                UUID candidateId = type.equals("PLACE_CANDIDATE")
                    ? primaryId
                    : link.candidateId();
                UUID sourceId = type.equals("RESEARCH_SOURCE")
                    ? primaryId
                    : link.sourceId();
                if (links.find(candidateId, sourceId).isPresent()) {
                    continue;
                }
                links.save(new CandidateSourceLinkStorePort.LinkRecord(
                    tripId, candidateId, sourceId, actorUserId, clock.instant()
                ));
                added.add(Map.of(
                    "candidateId", candidateId.toString(), "sourceId", sourceId.toString()
                ));
            }
        }
        return List.copyOf(added);
    }

    private static Map<String, Object> mergedPayload(
        PlanningResourceUseCase.ResourceResult primary,
        MergeCommand command
    ) {
        Map<String, Object> payload = new LinkedHashMap<>(primary.payload());
        if (command.mergedPayload() != null) {
            payload.putAll(command.mergedPayload());
        }
        payload.put("mergedResourceIds", command.duplicateIds().stream()
            .map(UUID::toString).toList());
        return Map.copyOf(payload);
    }

    private static Map<String, Object> snapshot(
        PlanningResourceUseCase.ResourceResult primary,
        List<PlanningResourceUseCase.ResourceResult> duplicates
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(primary.resourceId().toString(), resourceSnapshot(primary));
        duplicates.forEach(resource -> result.put(
            resource.resourceId().toString(), resourceSnapshot(resource)
        ));
        return Map.copyOf(result);
    }

    private static Map<String, Object> resourceSnapshot(
        PlanningResourceUseCase.ResourceResult resource
    ) {
        return Map.of(
            "payload", resource.payload(), "status", resource.status(),
            "sortOrder", resource.sortOrder(), "version", resource.version()
        );
    }

    private MergeResult currentResult(
        PlanningMergeStorePort.MergeRecord merge,
        UUID actorUserId
    ) {
        List<PlanningResourceUseCase.ResourceResult> current = currentResources(
            merge, actorUserId
        );
        return result(merge, current.getFirst(), current.subList(1, current.size()));
    }

    private List<PlanningResourceUseCase.ResourceResult> currentResources(
        PlanningMergeStorePort.MergeRecord merge,
        UUID actorUserId
    ) {
        List<PlanningResourceUseCase.ResourceResult> current = new ArrayList<>();
        current.add(resources.get(
            merge.tripId(), actorUserId, merge.resourceType(), merge.primaryId()
        ));
        merge.duplicateIds().forEach(id -> current.add(resources.get(
            merge.tripId(), actorUserId, merge.resourceType(), id
        )));
        return current;
    }

    private static MergeResult result(
        PlanningMergeStorePort.MergeRecord merge,
        PlanningResourceUseCase.ResourceResult primary,
        List<PlanningResourceUseCase.ResourceResult> duplicates
    ) {
        return new MergeResult(
            merge.id(), merge.resourceType(), merge.status(), primary,
            List.copyOf(duplicates), merge.mergedAt(), merge.revertedAt()
        );
    }

    private static void validate(MergeCommand command) {
        if (command == null || command.requestId() == null || command.primaryId() == null
            || command.duplicateIds() == null || command.duplicateIds().isEmpty()
            || command.duplicateBaseVersions() == null) {
            throw EarthTripException.badRequest(
                "INVALID_PLANNING_MERGE", "병합 ID, 대표 항목, 중복 항목과 기준 버전이 필요합니다."
            );
        }
        Set<UUID> ids = new LinkedHashSet<>(command.duplicateIds());
        if (ids.size() != command.duplicateIds().size() || ids.contains(command.primaryId())) {
            throw EarthTripException.badRequest(
                "INVALID_PLANNING_MERGE", "대표 항목과 겹치지 않는 중복 항목을 선택해 주세요."
            );
        }
    }

    private static void requireScope(
        PlanningMergeStorePort.MergeRecord merge,
        UUID tripId,
        String type
    ) {
        if (!merge.tripId().equals(tripId) || !merge.resourceType().equals(type)) {
            throw EarthTripException.conflict(
                "PLANNING_MERGE_SCOPE_CONFLICT", "다른 여행 또는 리소스의 병합 기록입니다."
            );
        }
    }

    private static Map<String, Object> resourceFrom(
        Map<String, Object> snapshot,
        UUID resourceId
    ) {
        Object value = snapshot.get(resourceId.toString());
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalStateException("병합 스냅샷에 리소스가 없습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static long versionFrom(Map<String, Object> snapshot, UUID resourceId) {
        return number(resourceFrom(snapshot, resourceId), "version").longValue();
    }

    private static Number number(Map<String, Object> value, String key) {
        Object number = value.get(key);
        if (!(number instanceof Number result)) {
            throw new IllegalStateException("병합 스냅샷의 숫자 필드가 올바르지 않습니다.");
        }
        return result;
    }

    private static String text(Map<String, Object> value, String key) {
        Object text = value.get(key);
        if (text == null) {
            throw new IllegalStateException("병합 스냅샷의 문자열 필드가 없습니다.");
        }
        return String.valueOf(text);
    }

    private static Map<String, Object> payloadFrom(Map<String, Object> snapshot) {
        Object value = snapshot.get("payload");
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalStateException("병합 스냅샷의 payload가 올바르지 않습니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return Map.copyOf(result);
    }

    private static void requireVersion(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT", 409, "다른 변경이 먼저 저장되어 병합할 수 없습니다.",
                Map.of("serverVersion", serverVersion)
            );
        }
    }
}
