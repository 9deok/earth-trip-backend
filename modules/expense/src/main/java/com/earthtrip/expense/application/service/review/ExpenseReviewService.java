package com.earthtrip.expense.application.service.review;

import com.earthtrip.expense.application.port.in.ExpenseReviewUseCase;
import com.earthtrip.expense.application.port.out.ExpenseReviewStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ExpenseReviewService implements ExpenseReviewUseCase {

    private final TripAccess access;
    private final ExpenseReviewStorePort store;
    private final Clock clock;

    ExpenseReviewService(TripAccess access, ExpenseReviewStorePort store, Clock clock) {
        this.access = access;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDayResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        return store.findAll(tripId).stream().map(ExpenseReviewService::result).toList();
    }

    @Override
    public ReviewDayResult complete(
        UUID tripId,
        LocalDate localDate,
        UUID actorUserId,
        String note,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        validateDate(tripId, localDate);
        ExpenseReviewStorePort.ReviewRecord current = store.find(tripId, localDate)
            .orElse(null);
        if (current != null && current.version() != baseVersion) {
            throw versionConflict(current.version());
        }
        if (current == null && baseVersion != 0) {
            throw versionConflict(0);
        }
        String normalizedNote = note == null || note.isBlank() ? null : note.strip();
        if (normalizedNote != null && normalizedNote.length() > 1_000) {
            throw EarthTripException.badRequest(
                "EXPENSE_REVIEW_NOTE_TOO_LONG", "검토 메모는 1000자 이하여야 합니다."
            );
        }
        return result(store.save(new ExpenseReviewStorePort.ReviewRecord(
            tripId, localDate, actorUserId, normalizedNote, clock.instant(),
            current == null ? 0 : current.version()
        )));
    }

    @Override
    public void reopen(
        UUID tripId,
        LocalDate localDate,
        UUID actorUserId,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        ExpenseReviewStorePort.ReviewRecord current = store.find(tripId, localDate)
            .orElse(null);
        if (current == null) {
            return;
        }
        if (current.version() != baseVersion) {
            throw versionConflict(current.version());
        }
        store.delete(tripId, localDate);
    }

    private void validateDate(UUID tripId, LocalDate localDate) {
        if (localDate == null) {
            throw EarthTripException.badRequest(
                "REVIEW_DATE_REQUIRED", "검토할 날짜가 필요합니다."
            );
        }
        TripAccess.PublicTripResult trip = access.publicInfo(tripId);
        if (trip.startDate() != null && trip.endDate() != null
            && (localDate.isBefore(trip.startDate()) || localDate.isAfter(trip.endDate()))) {
            throw EarthTripException.badRequest(
                "REVIEW_DATE_OUTSIDE_TRIP", "여행 날짜 안에서만 지출 검토를 완료할 수 있습니다."
            );
        }
    }

    private static ReviewDayResult result(ExpenseReviewStorePort.ReviewRecord record) {
        return new ReviewDayResult(
            record.localDate(), record.completedBy(), record.note(),
            record.completedAt(), record.version()
        );
    }

    private static EarthTripException versionConflict(long serverVersion) {
        return new EarthTripException(
            "VERSION_CONFLICT", 409, "다른 지출 검토 변경이 먼저 저장되었습니다.",
            Map.of("serverVersion", serverVersion)
        );
    }
}
