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
        return walletSnapshot(result);
    }

    @Override
    public BootstrapSnapshot bootstrap(UUID tripId, UUID actorUserId) {
        WalletRecordUseCase.WalletSummary result = wallet.wallet(tripId, actorUserId);
        List<WalletRecordUseCase.RecordResult> tasks =
                wallet.list(tripId, actorUserId, "PREPARATION_TASK", null);
        List<WalletRecordUseCase.RecordResult> packing =
                wallet.list(tripId, actorUserId, "PACKING_ITEM", null);
        return new BootstrapSnapshot(
                walletSnapshot(result), preparationSummary(result.reservations(), tasks, packing));
    }

    private static WalletSnapshot walletSnapshot(WalletRecordUseCase.WalletSummary result) {
        return new WalletSnapshot(
                entries(result.reservations()),
                entries(result.tickets()),
                result.offlineReadyCount(),
                result.actionRequiredCount());
    }

    @Override
    public PreparationSummary preparationSummary(UUID tripId, UUID actorUserId) {
        List<WalletRecordUseCase.RecordResult> reservations =
                wallet.list(tripId, actorUserId, "RESERVATION", null);
        List<WalletRecordUseCase.RecordResult> tasks =
                wallet.list(tripId, actorUserId, "PREPARATION_TASK", null);
        List<WalletRecordUseCase.RecordResult> packing =
                wallet.list(tripId, actorUserId, "PACKING_ITEM", null);
        return preparationSummary(reservations, tasks, packing);
    }

    private static PreparationSummary preparationSummary(
            List<WalletRecordUseCase.RecordResult> reservations,
            List<WalletRecordUseCase.RecordResult> tasks,
            List<WalletRecordUseCase.RecordResult> packing) {
        int completedTasks = completed(tasks);
        int completedPacking = completed(packing);
        int preparedReservations =
                (int)
                        reservations.stream()
                                .filter(TripWalletViewService::isActiveReservation)
                                .filter(
                                        record ->
                                                Boolean.TRUE.equals(
                                                        record.payload().get("walletSaved")))
                                .count();
        int activeReservations =
                (int)
                        reservations.stream()
                                .filter(TripWalletViewService::isActiveReservation)
                                .count();
        int total = tasks.size() + packing.size() + activeReservations;
        int complete = completedTasks + completedPacking + preparedReservations;
        return new PreparationSummary(
                complete,
                total,
                tasks.size() + packing.size() - completedTasks - completedPacking,
                tasks.stream()
                        .filter(record -> !isCompleted(record))
                        .limit(2)
                        .map(
                                record ->
                                        new PreparationItem(
                                                record.id(),
                                                record.payload(),
                                                record.status(),
                                                record.version()))
                        .toList());
    }

    private static List<Entry> entries(List<WalletRecordUseCase.RecordResult> records) {
        return records.stream()
                .map(
                        record ->
                                new Entry(
                                        record.id(),
                                        record.type(),
                                        record.payload(),
                                        record.status(),
                                        record.visibility(),
                                        record.version()))
                .toList();
    }

    private static int completed(List<WalletRecordUseCase.RecordResult> records) {
        return (int) records.stream().filter(TripWalletViewService::isCompleted).count();
    }

    private static boolean isCompleted(WalletRecordUseCase.RecordResult record) {
        return record.status().equals("COMPLETED")
                || Boolean.TRUE.equals(record.payload().get("isComplete"));
    }

    private static boolean isActiveReservation(WalletRecordUseCase.RecordResult record) {
        Object status = record.payload().get("status");
        return status == null
                ? !record.status().equals("DELETED") && !record.status().equals("CANCELLED")
                : String.valueOf(status).equalsIgnoreCase("active");
    }
}
