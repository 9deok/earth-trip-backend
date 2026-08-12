package com.earthtrip.planning.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TripPlanningView {

    PlanningSnapshot snapshot(UUID tripId, UUID actorUserId);

    NextDecision nextDecision(UUID tripId, UUID actorUserId);

    List<SearchEntry> searchEntries(UUID tripId, UUID actorUserId);

    record PlanningSnapshot(List<Day> days, Today today) {}

    record Day(UUID dayId, LocalDate localDate, int dayNumber, String timeZone) {}

    record Today(
            String state,
            LocalDate localDate,
            UUID dayId,
            String timeZone,
            List<ScheduleItem> items,
            int completedCount,
            int totalCount) {}

    record ScheduleItem(
            UUID itemId, Map<String, Object> payload, String status, int sortOrder, long version) {}

    record NextDecision(
            UUID candidateId,
            String title,
            int wantVotes,
            int holdVotes,
            int excludeVotes,
            long version) {}

    record SearchEntry(
            UUID id,
            String type,
            UUID parentId,
            LocalDate localDate,
            int sortOrder,
            Map<String, Object> payload,
            String status) {}
}
