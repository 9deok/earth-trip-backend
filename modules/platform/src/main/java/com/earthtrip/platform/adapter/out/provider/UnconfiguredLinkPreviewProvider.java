package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.LinkPreviewProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredLinkPreviewProvider implements LinkPreviewProviderPort {

    @Override
    public ProviderProxyUseCase.LinkPreviewResult preview(String url) {
        throw EarthTripException.unavailable(
            "LINK_PREVIEW_PROVIDER_NOT_CONFIGURED",
            "링크 미리보기 제공자가 설정되지 않았습니다."
        );
    }
}
