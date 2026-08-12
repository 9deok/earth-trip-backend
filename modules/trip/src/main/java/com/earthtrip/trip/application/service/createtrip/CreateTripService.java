package com.earthtrip.trip.application.service.createtrip;

import com.earthtrip.trip.api.TripChangePublisher;
import com.earthtrip.trip.application.port.in.CreateTripCommand;
import com.earthtrip.trip.application.port.in.CreateTripResult;
import com.earthtrip.trip.application.port.in.CreateTripUseCase;
import com.earthtrip.trip.application.port.out.LoadTripPort;
import com.earthtrip.trip.application.port.out.SaveTripPort;
import com.earthtrip.trip.domain.Trip;
import com.earthtrip.trip.domain.TripId;
import com.earthtrip.trip.domain.TripTitle;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CreateTripService implements CreateTripUseCase {

    private final LoadTripPort loadTripPort;
    private final SaveTripPort saveTripPort;
    private final TripChangePublisher changes;
    private final Clock clock;

    CreateTripService(
            LoadTripPort loadTripPort,
            SaveTripPort saveTripPort,
            TripChangePublisher changes,
            Clock clock) {
        this.loadTripPort = loadTripPort;
        this.saveTripPort = saveTripPort;
        this.changes = changes;
        this.clock = clock;
    }

    @Override
    public CreateTripResult create(CreateTripCommand command) {
        TripId tripId = new TripId(command.requestId());
        Trip trip = loadTripPort.findById(tripId).orElse(null);
        if (trip == null) {
            trip =
                    saveTripPort.save(
                            Trip.create(
                                    tripId,
                                    command.ownerUserId(),
                                    new TripTitle(command.title()),
                                    command.timeZone(),
                                    command.defaultCurrency(),
                                    clock.instant()));
            changes.publish(
                    trip.id().value(), command.ownerUserId(), "CREATED", "TRIP", trip.id().value());
        }

        if (!trip.isOwnedBy(command.ownerUserId())) {
            throw com.earthtrip.sharedkernel.error.EarthTripException.conflict(
                    "IDEMPOTENCY_KEY_REUSED", "다른 사용자가 이미 사용한 요청 ID입니다.");
        }

        return new CreateTripResult(trip.id().value(), trip.title().value(), trip.createdAt());
    }
}
