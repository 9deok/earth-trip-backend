package com.earthtrip.platform.application.service.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.out.ShareCredentialPort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SharedTripAccessServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 원본_여행이_삭제_대기면_공유_비밀번호_세션도_발급하지_않는다() {
        UUID tripId = UUID.randomUUID();
        UUID shareId = UUID.randomUUID();
        TripShareStorePort store = mock(TripShareStorePort.class);
        ShareCredentialPort credentials = mock(ShareCredentialPort.class);
        TripAccess tripAccess = mock(TripAccess.class);
        when(credentials.hashToken("share-token")).thenReturn("share-hash");
        when(credentials.matchesPassword("1234", "password-hash")).thenReturn(true);
        when(credentials.newToken()).thenReturn("session-token");
        when(store.findByTokenHash("share-hash"))
                .thenReturn(
                        Optional.of(
                                new TripShareStorePort.ShareRecord(
                                        shareId,
                                        tripId,
                                        "share-hash",
                                        "가족 공유",
                                        List.of("STRUCTURE"),
                                        "password-hash",
                                        UUID.randomUUID(),
                                        "LINK_ONLY",
                                        null,
                                        Map.of(),
                                        null,
                                        "ACTIVE",
                                        UUID.randomUUID(),
                                        NOW,
                                        NOW,
                                        null,
                                        0)));
        when(tripAccess.publicInfo(tripId))
                .thenThrow(EarthTripException.notFound("TRIP_NOT_FOUND", "여행을 찾을 수 없습니다."));
        SharedTripAccessService service =
                new SharedTripAccessService(
                        mock(TripStructureView.class),
                        mock(TripPlanningView.class),
                        mock(TripWalletView.class),
                        mock(TripExpenseView.class),
                        tripAccess,
                        store,
                        mock(ShareAccessRecorder.class),
                        credentials,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        mock(TripShareAuthorResolver.class));

        assertThatThrownBy(() -> service.verifyPassword("share-token", "1234"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("SHARED_TRIP_NOT_FOUND"));

        verify(store, never()).savePasswordSession(org.mockito.ArgumentMatchers.any());
    }
}
