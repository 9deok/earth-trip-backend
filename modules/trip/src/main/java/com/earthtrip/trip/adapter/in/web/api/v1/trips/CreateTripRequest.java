package com.earthtrip.trip.adapter.in.web.api.v1.trips;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record CreateTripRequest(
    @NotNull UUID requestId,
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 80) String timeZone,
    @NotBlank @Size(min = 3, max = 3) String defaultCurrency
) {
}
