package com.earthtrip.platform.adapter.in.web.internal.webhooks.financial_provider;

import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/webhooks/financial-provider")
class FinancialProviderWebhookController {

    private final InternalOperationsUseCase useCase;

    FinancialProviderWebhookController(InternalOperationsUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    InternalOperationsUseCase.WebhookResult post(
        @RequestHeader("X-EarthTrip-Webhook-Id") String eventId,
        @RequestHeader("X-EarthTrip-Webhook-Timestamp") String timestamp,
        @RequestHeader("X-EarthTrip-Webhook-Signature") String signature,
        @RequestBody String rawBody
    ) {
        return useCase.acceptWebhook(
            "financial-provider", eventId, timestamp, signature, rawBody
        );
    }
}
