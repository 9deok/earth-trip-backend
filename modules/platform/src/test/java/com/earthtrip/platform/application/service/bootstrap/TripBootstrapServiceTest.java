package com.earthtrip.platform.application.service.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.identity.api.TripMemberView;
import com.earthtrip.notification.api.TripNotificationView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.planning.api.TripSyncView;
import com.earthtrip.platform.application.port.in.TripBootstrapUseCase;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripBootstrapServiceTest {

    @Test
    void 홈_요약과_동기화_기준점을_한_응답으로_구성한다() {
        UUID tripId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-06T06:00:00Z");
        TripStructureView structure = mock(TripStructureView.class);
        TripMemberView members = mock(TripMemberView.class);
        TripPlanningView planning = mock(TripPlanningView.class);
        TripWalletView wallet = mock(TripWalletView.class);
        TripExpenseView expenses = mock(TripExpenseView.class);
        TripNotificationView notifications = mock(TripNotificationView.class);
        TripSyncView sync = mock(TripSyncView.class);
        TripStructureView.StructureSnapshot structureSnapshot =
                new TripStructureView.StructureSnapshot(null, List.of());
        TripPlanningView.PlanningSnapshot planningSnapshot =
                new TripPlanningView.PlanningSnapshot(List.of(), null);
        TripPlanningView.NextDecision nextDecision =
                new TripPlanningView.NextDecision(UUID.randomUUID(), "우에노 공원", 3, 1, 0, 4);
        TripWalletView.WalletSnapshot walletSnapshot =
                new TripWalletView.WalletSnapshot(List.of(), List.of(), 0, 0);
        TripWalletView.PreparationSummary preparation =
                new TripWalletView.PreparationSummary(3, 5, 2, List.of());
        TripWalletView.BootstrapSnapshot walletBootstrap =
                new TripWalletView.BootstrapSnapshot(walletSnapshot, preparation);
        TripExpenseView.ExpenseSummary expenseSummary =
                new TripExpenseView.ExpenseSummary(2, 1, List.of());
        when(structure.snapshot(tripId, actorId)).thenReturn(structureSnapshot);
        when(members.members(tripId, actorId)).thenReturn(List.of());
        when(planning.snapshot(tripId, actorId)).thenReturn(planningSnapshot);
        when(planning.nextDecision(tripId, actorId)).thenReturn(nextDecision);
        when(wallet.bootstrap(tripId, actorId)).thenReturn(walletBootstrap);
        when(expenses.summary(tripId, actorId)).thenReturn(expenseSummary);
        when(notifications.unreadCount(actorId, tripId)).thenReturn(7L);
        when(sync.latestCursor(tripId, actorId)).thenReturn(42L);
        TripBootstrapService service =
                new TripBootstrapService(
                        structure,
                        members,
                        planning,
                        wallet,
                        expenses,
                        notifications,
                        sync,
                        Clock.fixed(now, ZoneOffset.UTC));

        TripBootstrapUseCase.BootstrapResult result = service.get(tripId, actorId);

        assertThat(result.structure()).isSameAs(structureSnapshot);
        assertThat(result.planning()).isSameAs(planningSnapshot);
        assertThat(result.nextDecision()).isSameAs(nextDecision);
        assertThat(result.preparation()).isSameAs(preparation);
        assertThat(result.expenses()).isSameAs(expenseSummary);
        assertThat(result.unreadNotificationCount()).isEqualTo(7);
        assertThat(result.changeCursor()).isEqualTo(42);
        assertThat(result.generatedAt()).isEqualTo(now);
    }
}
