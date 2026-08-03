package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.WeatherProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredWeatherProvider implements WeatherProviderPort {

    @Override
    public List<ProviderProxyUseCase.WeatherDay> forecast(
        ProviderProxyUseCase.WeatherQuery query
    ) {
        throw EarthTripException.unavailable(
            "WEATHER_PROVIDER_NOT_CONFIGURED",
            "날씨 제공자가 설정되지 않았습니다."
        );
    }
}
