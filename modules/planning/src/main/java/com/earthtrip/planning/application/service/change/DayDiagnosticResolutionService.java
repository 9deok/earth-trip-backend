package com.earthtrip.planning.application.service.change;

import com.earthtrip.planning.application.port.in.DayDiagnosticResolutionUseCase;
import com.earthtrip.planning.application.port.in.RoutePlanningUseCase;
import com.earthtrip.planning.application.port.in.TripDayUseCase;
import com.earthtrip.planning.application.port.out.ScheduleChangeStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DayDiagnosticResolutionService implements DayDiagnosticResolutionUseCase {

    private final TripAccess access;
    private final TripDayUseCase days;
    private final RoutePlanningUseCase execution;
    private final ScheduleChangeStorePort store;
    private final Clock clock;

    DayDiagnosticResolutionService(
            TripAccess access,
            TripDayUseCase days,
            RoutePlanningUseCase execution,
            ScheduleChangeStorePort store,
            Clock clock) {
        this.access = access;
        this.days = days;
        this.execution = execution;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public ResolutionResult resolve(
            UUID tripId, UUID dayId, UUID diagnosticId, UUID actorUserId, String note) {
        access.requireEditor(tripId, actorUserId);
        days.requireDay(tripId, dayId, actorUserId);
        requireCurrentDiagnostic(tripId, dayId, diagnosticId, actorUserId);
        String normalizedNote = normalizeNote(note);
        ScheduleChangeStorePort.ResolutionRecord saved =
                store.saveResolution(
                        new ScheduleChangeStorePort.ResolutionRecord(
                                diagnosticId,
                                tripId,
                                dayId,
                                normalizedNote,
                                actorUserId,
                                clock.instant()));
        return result(saved);
    }

    @Override
    public void reopen(UUID tripId, UUID dayId, UUID diagnosticId, UUID actorUserId) {
        access.requireEditor(tripId, actorUserId);
        days.requireDay(tripId, dayId, actorUserId);
        ScheduleChangeStorePort.ResolutionRecord resolution =
                store.findResolution(diagnosticId)
                        .filter(item -> item.tripId().equals(tripId) && item.dayId().equals(dayId))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "DIAGNOSTIC_RESOLUTION_NOT_FOUND",
                                                "확인 처리된 일정 경고를 찾을 수 없습니다."));
        store.deleteResolution(resolution.diagnosticId());
    }

    private void requireCurrentDiagnostic(
            UUID tripId, UUID dayId, UUID diagnosticId, UUID actorUserId) {
        boolean exists =
                execution.diagnostics(tripId, dayId, actorUserId, null).stream()
                        .anyMatch(diagnostic -> diagnostic.diagnosticId().equals(diagnosticId));
        if (!exists) {
            throw EarthTripException.notFound(
                    "SCHEDULE_DIAGNOSTIC_NOT_FOUND", "현재 일정에서 해당 경고를 찾을 수 없습니다.");
        }
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String normalized = note.strip();
        if (normalized.length() > 1_000) {
            throw EarthTripException.badRequest(
                    "DIAGNOSTIC_RESOLUTION_NOTE_TOO_LONG", "경고 확인 메모는 1000자 이하여야 합니다.");
        }
        return normalized;
    }

    private static ResolutionResult result(ScheduleChangeStorePort.ResolutionRecord record) {
        return new ResolutionResult(
                record.diagnosticId(), record.note(), record.resolvedBy(), record.resolvedAt());
    }
}
