package com.earthtrip.trip.application.service.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.trip.application.port.in.TripSegmentUseCase;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.StructureProposal;
import com.earthtrip.trip.application.port.out.TripStructureSerializationPort;
import com.earthtrip.trip.application.port.out.TripStructureStorePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripStructureServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3");
    private static final UUID ACTOR = UUID.fromString("94d697a4-35c1-47da-a57c-72e26ed16826");
    private static final UUID REQUEST_ID = UUID.fromString("4d2dbb19-4ea8-4c7b-ae43-b7db2c34788a");
    private static final String HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Test
    void 적용_응답을_잃은_재시도는_낡은_여행_버전이어도_기존_결과를_반환한다() {
        TripAccess access = mock(TripAccess.class);
        TripStructureView structure = mock(TripStructureView.class);
        TripStructureStorePort store = mock(TripStructureStorePort.class);
        TripStructureSerializationPort serialization = mock(TripStructureSerializationPort.class);
        StructureProposal proposal =
                new StructureProposal(REQUEST_ID, 0, null, null, List.of(), List.of());
        TripStructureStorePort.ChangeSetRecord existing =
                new TripStructureStorePort.ChangeSetRecord(
                        REQUEST_ID, TRIP_ID, ACTOR, HASH, "{}", "{}", "APPLIED", NOW, null, 0);
        when(serialization.proposalHash(proposal)).thenReturn(HASH);
        when(store.changeSet(REQUEST_ID)).thenReturn(java.util.Optional.of(existing));
        TripStructureService service =
                new TripStructureService(
                        access,
                        structure,
                        mock(TripManagementUseCase.class),
                        mock(TripSegmentUseCase.class),
                        store,
                        serialization,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.apply(TRIP_ID, ACTOR, proposal, HASH);

        assertThat(result.changeSetId()).isEqualTo(REQUEST_ID);
        verify(structure, never()).snapshot(TRIP_ID, ACTOR);
    }
}
