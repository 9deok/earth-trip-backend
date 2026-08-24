package com.earthtrip.trip.application.service.segment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.out.TripSegmentStorePort;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripSegment;
import com.earthtrip.trip.spi.TripChangePublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripSegmentServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3");
    private static final UUID ACTOR = UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void 정렬_순서가_중복된_요청은_저장하지_않는다() {
        TripAccess access = mock(TripAccess.class);
        TripSegmentStorePort store = mock(TripSegmentStorePort.class);
        TripChangePublisher changes = mock(TripChangePublisher.class);
        TripSegment first = segment("4d2dbb19-4ea8-4c7b-ae43-b7db2c34788a", 0);
        TripSegment second = segment("6e3063a5-ad37-47ab-8f26-4f205461e7cd", 1);
        when(store.findAll(new TripId(TRIP_ID))).thenReturn(List.of(first, second));
        TripSegmentService service =
                new TripSegmentService(access, store, changes, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(
                        () ->
                                service.reorder(
                                        TRIP_ID,
                                        ACTOR,
                                        List.of(
                                                new TripSegmentUseCase.OrderItem(
                                                        first.id(), 0, first.version()),
                                                new TripSegmentUseCase.OrderItem(
                                                        second.id(), 0, second.version()))))
                .isInstanceOf(EarthTripException.class);

        verify(store, never()).save(any());
    }

    private static TripSegment segment(String id, int sortOrder) {
        return TripSegment.create(
                UUID.fromString(id),
                new TripId(TRIP_ID),
                TripSegment.Type.STAY,
                "도쿄",
                "JP",
                null,
                null,
                null,
                "Asia/Tokyo",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sortOrder,
                ACTOR,
                NOW);
    }
}
