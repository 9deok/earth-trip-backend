package com.earthtrip.identity.adapter.in.web.api.v1.me.preferences;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.PreferenceUseCase;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/preferences")
class PreferencesController {

    private final PreferenceUseCase useCase;
    private final CurrentUserProvider currentUser;

    PreferencesController(PreferenceUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    PreferenceResponse get() {
        return response(useCase.get(currentUser.requireUserId()));
    }

    @PatchMapping
    PreferenceResponse patch(@Valid @RequestBody PreferenceUpdateRequest request) {
        return response(
                useCase.update(
                        currentUser.requireUserId(),
                        new PreferenceUseCase.UpdatePreferenceCommand(
                                request.locale(),
                                request.defaultCurrency(),
                                request.timeZone(),
                                request.shareTicketNames(),
                                request.sharePersonalExpense(),
                                request.optionalAnalytics())));
    }

    private static PreferenceResponse response(PreferenceUseCase.PreferenceResult result) {
        return new PreferenceResponse(
                result.locale(),
                result.defaultCurrency(),
                result.timeZone(),
                result.shareTicketNames(),
                result.sharePersonalExpense(),
                result.optionalAnalytics(),
                result.version(),
                result.updatedAt());
    }
}

record PreferenceUpdateRequest(
        String locale,
        String defaultCurrency,
        String timeZone,
        Boolean shareTicketNames,
        Boolean sharePersonalExpense,
        Boolean optionalAnalytics) {}

record PreferenceResponse(
        String locale,
        String defaultCurrency,
        String timeZone,
        boolean shareTicketNames,
        boolean sharePersonalExpense,
        boolean optionalAnalytics,
        long version,
        Instant updatedAt) {}
