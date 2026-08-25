package com.earthtrip.platform.application.service.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicTripEngagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Test
    void 공개_여행에는_좋아요를_멱등하게_저장한다() {
        UUID publicationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        TripShareStorePort shares = mock(TripShareStorePort.class);
        PublicTripEngagementStorePort engagement = mock(PublicTripEngagementStorePort.class);
        when(shares.findById(publicationId))
                .thenReturn(Optional.of(share(publicationId, "PUBLIC")));
        when(engagement.hasReaction(publicationId, actorUserId, "LIKE")).thenReturn(false, true);
        when(engagement.countReactions(publicationId, "LIKE")).thenReturn(1L);
        when(engagement.countReactions(publicationId, "HELPFUL")).thenReturn(0L);
        when(engagement.countComments(publicationId)).thenReturn(0L);
        when(shares.accessEvents(publicationId)).thenReturn(List.of());
        PublicTripEngagementService service = service(shares, engagement);

        var result = service.setReaction(publicationId, actorUserId, "LIKE", true);

        ArgumentCaptor<PublicTripEngagementStorePort.ReactionRecord> saved =
                ArgumentCaptor.forClass(PublicTripEngagementStorePort.ReactionRecord.class);
        verify(engagement).saveReaction(saved.capture());
        assertThat(saved.getValue().publicationId()).isEqualTo(publicationId);
        assertThat(saved.getValue().actorUserId()).isEqualTo(actorUserId);
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(result.likedByMe()).isTrue();
    }

    @Test
    void 링크_전용_여행에는_공개_댓글을_남길_수_없다() {
        UUID publicationId = UUID.randomUUID();
        TripShareStorePort shares = mock(TripShareStorePort.class);
        PublicTripEngagementStorePort engagement = mock(PublicTripEngagementStorePort.class);
        when(shares.findById(publicationId))
                .thenReturn(Optional.of(share(publicationId, "LINK_ONLY")));
        PublicTripEngagementService service = service(shares, engagement);

        assertThatThrownBy(() -> service.addComment(publicationId, UUID.randomUUID(), "좋은 계획이에요"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        error -> assertThat(error.code()).isEqualTo("PUBLIC_TRIP_NOT_FOUND"));
        verify(engagement, never()).saveComment(any());
    }

    private static PublicTripEngagementService service(
            TripShareStorePort shares, PublicTripEngagementStorePort engagement) {
        return new PublicTripEngagementService(
                shares,
                engagement,
                mock(ShareAccessRecorder.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TripShareStorePort.ShareRecord share(UUID publicationId, String visibility) {
        return new TripShareStorePort.ShareRecord(
                publicationId,
                UUID.randomUUID(),
                "token-hash",
                "공개 여행",
                List.of("STRUCTURE", "ITINERARY"),
                null,
                UUID.randomUUID(),
                visibility,
                "여행 팁",
                Map.of(),
                null,
                "ACTIVE",
                UUID.randomUUID(),
                NOW,
                NOW,
                null,
                0);
    }
}
