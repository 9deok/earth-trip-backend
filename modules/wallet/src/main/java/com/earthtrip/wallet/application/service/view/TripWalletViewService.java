package com.earthtrip.wallet.application.service.view;

import com.earthtrip.wallet.api.TripWalletView;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripWalletViewService implements TripWalletView {

    private final WalletRecordUseCase wallet;

    TripWalletViewService(WalletRecordUseCase wallet) {
        this.wallet = wallet;
    }

    @Override
    public WalletSnapshot snapshot(UUID tripId, UUID actorUserId) {
        WalletRecordUseCase.WalletSummary result = wallet.wallet(tripId, actorUserId);
        return new WalletSnapshot(
            entries(result.reservations()),
            entries(result.tickets()),
            result.offlineReadyCount(),
            result.actionRequiredCount()
        );
    }

    private static List<Entry> entries(List<WalletRecordUseCase.RecordResult> records) {
        return records.stream()
            .map(record -> new Entry(
                record.id(), record.type(), record.payload(), record.status(),
                record.visibility(), record.version()
            ))
            .toList();
    }
}
