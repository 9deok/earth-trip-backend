package com.earthtrip.wallet.application.port.out;

import java.util.Map;

public interface ReservationProposalFingerprintPort {
    String fingerprint(Map<String, Object> proposal);
}
