package com.earthtrip.wallet.application.service.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.spi.TripChangePublisher;
import com.earthtrip.wallet.application.port.out.WalletRecordStorePort;
import com.earthtrip.wallet.domain.WalletRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 다른_사용자의_비공개_지갑_항목은_ID를_알아도_삭제할_수_없다() {
        UUID tripId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        WalletRecord record =
                WalletRecord.create(
                        recordId,
                        tripId,
                        "WALLET_ENTRY",
                        null,
                        Map.of("title", "개인 메모"),
                        "ACTIVE",
                        "PRIVATE",
                        0,
                        owner,
                        NOW);
        WalletRecordStorePort store = mock(WalletRecordStorePort.class);
        when(store.findById(recordId)).thenReturn(Optional.of(record));
        WalletRecordService service =
                new WalletRecordService(
                        mock(TripAccess.class),
                        store,
                        mock(TripChangePublisher.class),
                        Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(
                        () -> service.delete(tripId, attacker, "WALLET_ENTRY", recordId, false, 0))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("WALLET_RECORD_NOT_FOUND"));

        verify(store, never()).save(record);
    }
}
