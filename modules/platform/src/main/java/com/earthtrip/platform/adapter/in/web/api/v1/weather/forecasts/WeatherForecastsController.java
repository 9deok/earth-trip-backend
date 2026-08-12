package com.earthtrip.platform.adapter.in.web.api.v1.weather.forecasts;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather/forecasts")
class WeatherForecastsController {

    private final ProviderProxyUseCase useCase;

    WeatherForecastsController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<ProviderProxyUseCase.WeatherDay> get(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam String timeZone) {
        return useCase.weather(
                new ProviderProxyUseCase.WeatherQuery(
                        latitude, longitude, startDate, endDate, timeZone));
    }
}
