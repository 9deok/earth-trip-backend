package com.earthtrip.platform.application.service.share;

import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.PublicTripDiscoveryUseCase;
import com.earthtrip.platform.application.port.out.PublicTripEngagementStorePort;
import com.earthtrip.platform.application.port.out.TripShareStorePort;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class PublicTripDiscoveryService implements PublicTripDiscoveryUseCase {

    private final TripShareStorePort shares;
    private final TripAccess trips;
    private final TripStructureView structure;
    private final TripPlanningView planning;
    private final PublicTripEngagementStorePort engagement;
    private final TripShareAuthorResolver authors;

    PublicTripDiscoveryService(
            TripShareStorePort shares,
            TripAccess trips,
            TripStructureView structure,
            TripPlanningView planning,
            PublicTripEngagementStorePort engagement,
            TripShareAuthorResolver authors) {
        this.shares = shares;
        this.trips = trips;
        this.structure = structure;
        this.planning = planning;
        this.engagement = engagement;
        this.authors = authors;
    }

    @Override
    public List<PublicTripSummary> discover(String destination, int limit) {
        String filter = destination == null ? "" : destination.strip().toLowerCase(Locale.ROOT);
        return shares.findPublic(50).stream()
                .map(this::summary)
                .filter(summary -> matches(summary, filter))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private PublicTripSummary summary(TripShareStorePort.ShareRecord share) {
        TripAccess.PublicTripResult trip = trips.publicInfo(share.tripId());
        TripStructureView.StructureSnapshot snapshot =
                structure.snapshot(share.tripId(), share.projectionUserId());
        List<String> cities =
                snapshot.segments().stream()
                        .filter(
                                segment ->
                                        segment.type() == null
                                                || !segment.type().contains("TRANSFER"))
                        .map(TripStructureView.Segment::cityName)
                        .filter(name -> name != null && !name.isBlank())
                        .distinct()
                        .toList();
        int itineraryCount =
                share.scopes().contains("ITINERARY")
                        ? planning.searchEntries(share.tripId(), share.projectionUserId()).stream()
                                .filter(entry -> entry.type().equals("SCHEDULE_ITEM"))
                                .toList()
                                .size()
                        : 0;
        long viewCount =
                shares.accessEvents(share.id()).stream()
                        .filter(TripShareStorePort.AccessRecord::success)
                        .filter(event -> event.reason().startsWith("OPENED"))
                        .count();
        long copyCount =
                shares.accessEvents(share.id()).stream()
                        .filter(TripShareStorePort.AccessRecord::success)
                        .filter(event -> event.reason().equals("COPIED_PUBLIC"))
                        .count();
        return new PublicTripSummary(
                share.id(),
                trip.title(),
                authors.displayName(share),
                share.publicNote(),
                trip.startDate(),
                trip.endDate(),
                cities,
                itineraryCount,
                viewCount,
                engagement.countReactions(share.id(), "LIKE"),
                engagement.countReactions(share.id(), "HELPFUL"),
                engagement.countComments(share.id()),
                copyCount,
                share.publicContent().get("heroPhotoUrl"),
                share.updatedAt());
    }

    private static boolean matches(PublicTripSummary summary, String destination) {
        if (destination.isEmpty()) {
            return true;
        }
        return summary.cities().stream()
                .map(city -> city.toLowerCase(Locale.ROOT))
                .anyMatch(city -> city.contains(destination) || destination.contains(city));
    }
}
