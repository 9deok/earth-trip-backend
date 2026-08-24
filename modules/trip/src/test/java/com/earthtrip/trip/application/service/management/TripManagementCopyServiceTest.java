package com.earthtrip.trip.application.service.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import com.earthtrip.trip.spi.TripChangePublisher;
import com.earthtrip.trip.spi.TripMembershipLookup;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TripManagementCopyServiceTest {

    private static final UUID SOURCE_ID = UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3");
    private static final UUID COPY_ID = UUID.fromString("4d2dbb19-4ea8-4c7b-ae43-b7db2c34788a");
    private static final UUID ACTOR = UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826");
    private static final UUID SEGMENT_ID = UUID.fromString("6e3063a5-ad37-47ab-8f26-4f205461e7cd");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void 여행_복사는_체류와_이동의_시간_맥락까지_새_ID로_복제한다() {
        LoadTripPort load = mock(LoadTripPort.class);
        SaveTripPort save = mock(SaveTripPort.class);
        TripSegmentUseCase segments = mock(TripSegmentUseCase.class);
        Trip source =
                Trip.create(
                        new TripId(SOURCE_ID),
                        ACTOR,
                        new TripTitle("하노이 여행"),
                        "Asia/Ho_Chi_Minh",
                        "VND",
                        NOW);
        when(load.findById(new TripId(SOURCE_ID))).thenReturn(Optional.of(source));
        when(load.findById(new TripId(COPY_ID))).thenReturn(Optional.empty());
        when(save.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant anchorAt = Instant.parse("2026-09-01T11:30:00Z");
        when(segments.list(SOURCE_ID, ACTOR))
                .thenReturn(
                        List.of(
                                new TripSegmentUseCase.SegmentResult(
                                        SEGMENT_ID,
                                        SOURCE_ID,
                                        "TRANSFER",
                                        "공항 → 하노이",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "Asia/Ho_Chi_Minh",
                                        LocalDate.of(2026, 9, 1),
                                        LocalDate.of(2026, 9, 1),
                                        null,
                                        null,
                                        null,
                                        null,
                                        "BUS",
                                        Instant.parse("2026-09-01T12:00:00Z"),
                                        Instant.parse("2026-09-01T13:00:00Z"),
                                        anchorAt,
                                        0,
                                        0,
                                        ACTOR,
                                        NOW)));
        TripManagementService service =
                new TripManagementService(
                        load,
                        save,
                        mock(TripMembershipLookup.class),
                        mock(TripAccess.class),
                        mock(TripChangePublisher.class),
                        segments,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.copy(SOURCE_ID, ACTOR, COPY_ID, "하노이 여행 복사본");

        assertThat(result.tripId()).isEqualTo(COPY_ID);
        ArgumentCaptor<TripSegmentUseCase.SegmentCommand> command =
                ArgumentCaptor.forClass(TripSegmentUseCase.SegmentCommand.class);
        verify(segments)
                .create(
                        org.mockito.ArgumentMatchers.eq(COPY_ID),
                        org.mockito.ArgumentMatchers.eq(ACTOR),
                        command.capture());
        assertThat(command.getValue().requestId()).isNotEqualTo(SEGMENT_ID);
        assertThat(command.getValue().timeZone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(command.getValue().anchorAt()).isEqualTo(anchorAt);
    }

    @Test
    void 기존의_다른_여행_ID를_복사_요청_ID로_재사용할_수_없다() {
        LoadTripPort load = mock(LoadTripPort.class);
        SaveTripPort save = mock(SaveTripPort.class);
        TripSegmentUseCase segments = mock(TripSegmentUseCase.class);
        Trip source =
                Trip.create(
                        new TripId(SOURCE_ID),
                        ACTOR,
                        new TripTitle("하노이 여행"),
                        "Asia/Ho_Chi_Minh",
                        "VND",
                        NOW);
        Trip unrelated =
                Trip.create(
                        new TripId(COPY_ID),
                        ACTOR,
                        new TripTitle("내 기존 여행"),
                        "Asia/Seoul",
                        "KRW",
                        NOW);
        when(load.findById(new TripId(SOURCE_ID))).thenReturn(Optional.of(source));
        when(load.findById(new TripId(COPY_ID))).thenReturn(Optional.of(unrelated));
        TripManagementService service =
                new TripManagementService(
                        load,
                        save,
                        mock(TripMembershipLookup.class),
                        mock(TripAccess.class),
                        mock(TripChangePublisher.class),
                        segments,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.copy(SOURCE_ID, ACTOR, COPY_ID, "하노이 여행 복사본"))
                .isInstanceOf(com.earthtrip.sharedkernel.error.EarthTripException.class)
                .hasMessageContaining("이미 사용된 요청 ID");
        verify(segments, never()).create(any(), any(), any());
    }
}
