package com.earthtrip.platform.adapter.in.web.api.v1.link_preview_queries;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/link-preview-queries")
class LinkPreviewQueriesController {

    private final ProviderProxyUseCase useCase;

    LinkPreviewQueriesController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    ProviderProxyUseCase.LinkPreviewResult post(
        @Valid @RequestBody LinkPreviewRequest request
    ) {
        return useCase.linkPreview(request.url());
    }
}

record LinkPreviewRequest(@NotBlank @Size(max = 2048) String url) { }
