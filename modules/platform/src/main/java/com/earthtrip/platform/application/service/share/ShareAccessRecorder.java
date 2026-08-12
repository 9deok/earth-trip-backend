package com.earthtrip.platform.application.service.share;

import com.earthtrip.platform.application.port.out.TripShareStorePort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ShareAccessRecorder {

    private final TripShareStorePort store;
    private final Clock clock;

    ShareAccessRecorder(TripShareStorePort store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(UUID shareId, boolean success, String reason) {
        store.appendAccess(
                new TripShareStorePort.AccessRecord(
                        UUID.randomUUID(), shareId, success, reason, clock.instant()));
    }
}
