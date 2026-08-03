package com.earthtrip.platform.application.port.in;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.identity.api.TripMemberView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TripBootstrapUseCase {

    BootstrapResult get(UUID tripId, UUID actorUserId);

    record BootstrapResult(
        TripStructureView.StructureSnapshot structure,
        List<TripMemberView.Member> members,
        TripPlanningView.PlanningSnapshot planning,
        TripWalletView.WalletSnapshot wallet,
        TripExpenseView.ExpenseSummary expenses,
        Instant generatedAt
    ) { }
}
