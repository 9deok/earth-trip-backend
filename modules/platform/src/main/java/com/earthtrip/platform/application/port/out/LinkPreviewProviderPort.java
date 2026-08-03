package com.earthtrip.platform.application.port.out;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;

public interface LinkPreviewProviderPort {

    ProviderProxyUseCase.LinkPreviewResult preview(String url);
}
