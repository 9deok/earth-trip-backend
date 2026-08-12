package com.earthtrip.trip.application.port.out;

import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.trip.application.port.in.TripStructureUseCase;

public interface TripStructureSerializationPort {
    String proposalHash(TripStructureUseCase.StructureProposal proposal);

    String serialize(TripStructureView.StructureSnapshot snapshot);

    TripStructureView.StructureSnapshot deserialize(String value);
}
