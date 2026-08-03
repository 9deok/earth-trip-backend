package com.earthtrip.platform.adapter.in.web.api.v1.app_capabilities;

import com.earthtrip.platform.application.port.in.PlatformInfoUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app-capabilities")
class AppCapabilitiesController {

    private final PlatformInfoUseCase useCase;

    AppCapabilitiesController(PlatformInfoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    AppCapabilitiesResponse get() {
        PlatformInfoUseCase.AppCapabilities result = useCase.appCapabilities();
        return new AppCapabilitiesResponse(
            result.apiVersion(), result.minimumAndroidBuild(), result.minimumIosBuild(),
            result.maintenanceMode(), result.maintenanceMessage(), result.readOnlyAvailable(),
            result.providers().stream()
                .map(provider -> new ProviderCapabilityResponse(
                    provider.provider(), provider.available(), provider.status()
                ))
                .toList()
        );
    }
}

record AppCapabilitiesResponse(
    String apiVersion,
    int minimumAndroidBuild,
    int minimumIosBuild,
    boolean maintenanceMode,
    String maintenanceMessage,
    boolean readOnlyAvailable,
    List<ProviderCapabilityResponse> providers
) { }

record ProviderCapabilityResponse(String provider, boolean available, String status) { }
