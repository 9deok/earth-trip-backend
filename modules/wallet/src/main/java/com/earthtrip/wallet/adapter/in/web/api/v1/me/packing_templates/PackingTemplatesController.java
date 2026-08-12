package com.earthtrip.wallet.adapter.in.web.api.v1.me.packing_templates;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/packing-templates")
class PackingTemplatesController {

    private final PackingTemplateUseCase useCase;
    private final CurrentActor actor;

    PackingTemplatesController(PackingTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    List<PackingTemplateUseCase.TemplateResult> get() {
        return useCase.list(actor.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PackingTemplateUseCase.TemplateResult post(@Valid @RequestBody PackingTemplateRequest request) {
        return useCase.create(actor.requireUserId(), request.toCommand());
    }
}

record PackingTemplateRequest(
        @NotNull UUID requestId,
        @NotBlank @Size(max = 120) String name,
        String visibility,
        @NotNull @Size(max = 200) @Valid List<PackingTemplateItemRequest> items) {
    PackingTemplateUseCase.TemplateCommand toCommand() {
        return new PackingTemplateUseCase.TemplateCommand(
                requestId,
                name,
                visibility,
                items.stream().map(PackingTemplateItemRequest::toItem).toList(),
                0);
    }
}

record PackingTemplateItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String category,
        @Min(1) @Max(999) int quantity,
        @Size(max = 500) String note) {
    PackingTemplateUseCase.TemplateItem toItem() {
        return new PackingTemplateUseCase.TemplateItem(name, category, quantity, note);
    }
}
