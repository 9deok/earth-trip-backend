package com.earthtrip.trip.application.service.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.application.port.in.TripStructureUseCase.StructureProposal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripStructureProposalPolicyTest {

    @Test
    void 비어있는_목록을_불변_목록으로_정규화한다() {
        UUID requestId = UUID.randomUUID();
        StructureProposal normalized =
                TripStructureProposalPolicy.validate(
                        new StructureProposal(requestId, 0, null, null, null, null));

        assertThat(normalized.requestId()).isEqualTo(requestId);
        assertThat(normalized.segments()).isEmpty();
        assertThat(normalized.removedSegments()).isEmpty();
    }

    @Test
    void 종료일이_시작일보다_빠른_제안을_거부한다() {
        StructureProposal proposal =
                new StructureProposal(
                        UUID.randomUUID(),
                        0,
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 9),
                        List.of(),
                        List.of());

        assertThatThrownBy(() -> TripStructureProposalPolicy.validate(proposal))
                .isInstanceOf(EarthTripException.class);
    }
}
