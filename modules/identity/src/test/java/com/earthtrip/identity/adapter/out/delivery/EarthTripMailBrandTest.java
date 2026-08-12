package com.earthtrip.identity.adapter.out.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EarthTripMailBrandTest {

    @Test
    void rendersCanonicalLogoAndWordmark() {
        String header = EarthTripMailBrand.header("https://earth-trips.com/");

        assertThat(header)
                .contains("https://earth-trips.com/brand/earth-trip-mark.png")
                .contains("earth-trip")
                .contains("#3a2a27");
    }
}
