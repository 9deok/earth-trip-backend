package com.earthtrip.platform.application.service.bootstrap;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.identity.api.TripMemberView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.TripBootstrapUseCase;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripBootstrapService implements TripBootstrapUseCase {

    private final TripStructureView structure;
    private final TripMemberView members;
    private final TripPlanningView planning;
    private final TripWalletView wallet;
    private final TripExpenseView expenses;
    private final Clock clock;

    TripBootstrapService(
        TripStructureView structure,
        TripMemberView members,
        TripPlanningView planning,
        TripWalletView wallet,
        TripExpenseView expenses,
        Clock clock
    ) {
        this.structure = structure;
        this.members = members;
        this.planning = planning;
        this.wallet = wallet;
        this.expenses = expenses;
        this.clock = clock;
    }

    @Override
    public BootstrapResult get(UUID tripId, UUID actorUserId) {
        return new BootstrapResult(
            structure.snapshot(tripId, actorUserId),
            members.members(tripId, actorUserId),
            planning.snapshot(tripId, actorUserId),
            wallet.snapshot(tripId, actorUserId),
            expenses.summary(tripId, actorUserId),
            clock.instant()
        );
    }
}
