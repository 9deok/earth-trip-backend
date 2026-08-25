package com.earthtrip.platform.application.service.share;

import java.time.Duration;
import java.util.Set;

final class TripSharePolicy {
    static final Set<String> SCOPES =
            Set.of("STRUCTURE", "ITINERARY", "RESERVATIONS", "BUDGET_SUMMARY");
    static final Set<String> VISIBILITIES = Set.of("LINK_ONLY", "PUBLIC");
    static final Set<String> PUBLIC_CONTENT_FIELDS =
            Set.of(
                    "overviewNote",
                    "itineraryNote",
                    "transportTip",
                    "lodgingTip",
                    "budgetSummary",
                    "packingTip",
                    "contingencyPlan",
                    "heroPhotoUrl",
                    "galleryPhotoUrl");
    static final Set<String> PUBLIC_RESERVATION_FIELDS =
            Set.of(
                    "title",
                    "name",
                    "provider",
                    "type",
                    "category",
                    "startAt",
                    "endAt",
                    "address",
                    "status");
    static final Set<String> PUBLIC_PLANNING_FIELDS =
            Set.of(
                    "title",
                    "name",
                    "startMinute",
                    "endMinute",
                    "address",
                    "latitude",
                    "longitude",
                    "fixedTime",
                    "authorNote",
                    "travelMinutesFromPrevious",
                    "travelModeFromPrevious",
                    "transferDepartureMinutesFromPrevious",
                    "transferArrivalMinutesFromPrevious",
                    "overnightFromPrevious",
                    "transferReservationConfirmed");
    static final Duration PASSWORD_SESSION_TTL = Duration.ofMinutes(15);

    private TripSharePolicy() {}
}
