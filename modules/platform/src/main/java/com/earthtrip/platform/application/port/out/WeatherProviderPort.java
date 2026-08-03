package com.earthtrip.platform.application.port.out;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.util.List;

public interface WeatherProviderPort {

    List<ProviderProxyUseCase.WeatherDay> forecast(ProviderProxyUseCase.WeatherQuery query);
}
