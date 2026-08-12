package com.earthtrip.platform.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.Test;

class GoogleGeoTravelClientSecurityTest {

    @Test
    void google처럼_보이는_공격자_도메인을_리디렉션_대상으로_허용하지_않는다() throws Exception {
        Method validator =
                GoogleGeoTravelClient.class.getDeclaredMethod("requireGoogleMapsHost", URI.class);
        validator.setAccessible(true);

        assertThatThrownBy(
                        () ->
                                validator.invoke(
                                        null,
                                        URI.create(
                                                "https://maps.google.attacker.example/internal")))
                .isInstanceOfSatisfying(
                        InvocationTargetException.class,
                        error ->
                                assertThat(error.getCause())
                                        .isInstanceOfSatisfying(
                                                EarthTripException.class,
                                                cause ->
                                                        assertThat(cause.code())
                                                                .isEqualTo(
                                                                        "UNSUPPORTED_PLACE_URL_PROVIDER")));

        assertThatCode(
                        () ->
                                validator.invoke(
                                        null,
                                        URI.create("https://maps.google.com/maps/place/Seoul")))
                .doesNotThrowAnyException();
    }
}
