package com.earthtrip.wallet.adapter.in.web.api.v1.me.packing_templates.by_template_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/packing-templates/{templateId}")
class PackingTemplateByIdController {

    private final PackingTemplateUseCase useCase;
    private final CurrentActor actor;

    PackingTemplateByIdController(PackingTemplateUseCase useCase, CurrentActor actor) {
        this.useCase = useCase;
        this.actor = actor;
    }

    @GetMapping
    PackingTemplateUseCase.TemplateResult get(@PathVariable UUID templateId) {
        return useCase.get(templateId, actor.requireUserId());
    }

    @PatchMapping
    PackingTemplateUseCase.TemplateResult patch(
            @PathVariable UUID templateId,
            @Valid @RequestBody PackingTemplatePatchRequest request) {
        return useCase.update(templateId, actor.requireUserId(), request.toCommand());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID templateId, @RequestParam @PositiveOrZero long baseVersion) {
        useCase.delete(templateId, actor.requireUserId(), baseVersion);
    }
}

record PackingTemplatePatchRequest(
        @Size(min = 1, max = 120) String name,
        String visibility,
        @Size(max = 200) @Valid List<PackingTemplateItemPatchRequest> items,
        @PositiveOrZero long baseVersion) {
    PackingTemplateUseCase.TemplateCommand toCommand() {
        return new PackingTemplateUseCase.TemplateCommand(
                null,
                name,
                visibility,
                items == null
                        ? null
                        : items.stream().map(PackingTemplateItemPatchRequest::toItem).toList(),
                baseVersion);
    }
}

record PackingTemplateItemPatchRequest(
        @Size(min = 1, max = 120) String name,
        @Size(max = 60) String category,
        @Min(1) @Max(999) int quantity,
        @Size(max = 500) String note) {
    PackingTemplateUseCase.TemplateItem toItem() {
        return new PackingTemplateUseCase.TemplateItem(name, category, quantity, note);
    }
}
