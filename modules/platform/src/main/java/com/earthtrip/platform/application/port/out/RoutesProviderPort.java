package com.earthtrip.platform.application.port.out;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;

public interface RoutesProviderPort {

    ProviderProxyUseCase.RouteResult route(ProviderProxyUseCase.RouteQuery query);

    ProviderProxyUseCase.MatrixResult matrix(ProviderProxyUseCase.MatrixQuery query);
}
