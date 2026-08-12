package com.earthtrip.trip.application.service.structure;

import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;
import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StructureDiagnosticCalculator {

    private StructureDiagnosticCalculator() {}

    static List<TripStructureUseCase.DiagnosticResult> calculate(
            UUID tripId,
            LocalDate tripStart,
            LocalDate tripEnd,
            List<TripStructureView.Segment> segments,
            Map<UUID, TripStructureStorePort.ResolutionRecord> resolutions) {
        List<RawDiagnostic> raw = new ArrayList<>();
        if (tripStart == null || tripEnd == null) {
            raw.add(
                    new RawDiagnostic(
                            "INCOMPLETE_TRIP_DATES",
                            "ERROR",
                            "여행 시작일과 종료일을 모두 정해야 숙박 구조를 검사할 수 있습니다.",
                            null,
                            List.of()));
        }
        for (TripStructureView.Segment segment : segments) {
            addSegmentDiagnostics(raw, tripStart, tripEnd, segment);
        }
        if (tripStart != null && tripEnd != null && !tripEnd.isBefore(tripStart)) {
            addNightDiagnostics(raw, tripStart, tripEnd, segments);
        }
        return raw.stream().map(diagnostic -> result(tripId, diagnostic, resolutions)).toList();
    }

    private static void addSegmentDiagnostics(
            List<RawDiagnostic> diagnostics,
            LocalDate tripStart,
            LocalDate tripEnd,
            TripStructureView.Segment segment) {
        if (tripStart != null
                && tripEnd != null
                && (segment.startDate().isBefore(tripStart)
                        || segment.endDate().isAfter(tripEnd))) {
            diagnostics.add(
                    new RawDiagnostic(
                            "SEGMENT_OUTSIDE_TRIP_DATES",
                            "ERROR",
                            "여행 날짜 범위를 벗어난 구간이 있습니다.",
                            segment.startDate(),
                            List.of(segment.segmentId())));
        }
        if (segment.type().equals("STAY")
                && (segment.accommodationName() == null || segment.accommodationName().isBlank())) {
            diagnostics.add(
                    new RawDiagnostic(
                            "MISSING_ACCOMMODATION",
                            "WARNING",
                            "체류 구간에 숙소가 지정되지 않았습니다.",
                            segment.startDate(),
                            List.of(segment.segmentId())));
        }
        if (!segment.type().equals("STAY")
                && (segment.transportMode() == null || segment.transportMode().isBlank())) {
            diagnostics.add(
                    new RawDiagnostic(
                            "MISSING_TRANSFER_MODE",
                            "WARNING",
                            "이동 구간의 교통수단이 지정되지 않았습니다.",
                            segment.startDate(),
                            List.of(segment.segmentId())));
        }
        if (!segment.type().equals("STAY")
                && (segment.departureAt() == null || segment.arrivalAt() == null)) {
            diagnostics.add(
                    new RawDiagnostic(
                            "INCOMPLETE_TRANSFER_TIME",
                            "WARNING",
                            "이동 구간의 출발·도착 시각이 완전하지 않습니다.",
                            segment.startDate(),
                            List.of(segment.segmentId())));
        }
    }

    private static void addNightDiagnostics(
            List<RawDiagnostic> diagnostics,
            LocalDate tripStart,
            LocalDate tripEnd,
            List<TripStructureView.Segment> segments) {
        for (LocalDate night = tripStart; night.isBefore(tripEnd); night = night.plusDays(1)) {
            LocalDate currentNight = night;
            List<TripStructureView.Segment> stays =
                    segments.stream()
                            .filter(segment -> segment.type().equals("STAY"))
                            .filter(segment -> !currentNight.isBefore(segment.startDate()))
                            .filter(segment -> currentNight.isBefore(segment.endDate()))
                            .sorted(Comparator.comparing(TripStructureView.Segment::sortOrder))
                            .toList();
            boolean overnightTransfer =
                    segments.stream()
                            .filter(segment -> segment.type().equals("OVERNIGHT_TRANSFER"))
                            .anyMatch(
                                    segment ->
                                            !currentNight.isBefore(segment.startDate())
                                                    && currentNight.isBefore(segment.endDate()));
            if (stays.isEmpty() && !overnightTransfer) {
                diagnostics.add(
                        new RawDiagnostic(
                                "EMPTY_NIGHT",
                                "ERROR",
                                "숙소나 의도한 야간 이동이 없는 밤이 있습니다.",
                                night,
                                List.of()));
            }
            if (stays.size() > 1) {
                diagnostics.add(
                        new RawDiagnostic(
                                "OVERLAPPING_ACCOMMODATION",
                                "ERROR",
                                "같은 밤에 숙소가 중복되어 있습니다.",
                                night,
                                stays.stream().map(TripStructureView.Segment::segmentId).toList()));
            }
        }
    }

    private static TripStructureUseCase.DiagnosticResult result(
            UUID tripId,
            RawDiagnostic raw,
            Map<UUID, TripStructureStorePort.ResolutionRecord> resolutions) {
        UUID id = diagnosticId(tripId, raw);
        TripStructureStorePort.ResolutionRecord resolution = resolutions.get(id);
        return new TripStructureUseCase.DiagnosticResult(
                id,
                raw.code(),
                raw.severity(),
                raw.message(),
                raw.localDate(),
                raw.segmentIds(),
                resolution != null,
                resolution == null ? null : resolution.note(),
                resolution == null ? null : resolution.resolvedBy(),
                resolution == null ? null : resolution.resolvedAt());
    }

    private static UUID diagnosticId(UUID tripId, RawDiagnostic diagnostic) {
        String segmentKey =
                diagnostic.segmentIds().stream()
                        .map(UUID::toString)
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(","));
        String source =
                tripId + "|" + diagnostic.code() + "|" + diagnostic.localDate() + "|" + segmentKey;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private record RawDiagnostic(
            String code,
            String severity,
            String message,
            LocalDate localDate,
            List<UUID> segmentIds) {}
}
