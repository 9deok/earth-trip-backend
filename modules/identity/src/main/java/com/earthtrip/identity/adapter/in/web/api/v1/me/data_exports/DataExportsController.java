package com.earthtrip.identity.adapter.in.web.api.v1.me.data_exports;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.DataExportUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/me/data-exports")
class DataExportsController {

    private final DataExportUseCase useCase;
    private final CurrentUserProvider currentUser;

    DataExportsController(DataExportUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<DataExportUseCase.ExportResult> get() {
        return useCase.list(currentUser.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DataExportUseCase.ExportResult post(@Valid @RequestBody DataExportRequest request) {
        return useCase.create(
            currentUser.requireUserId(), request.requestId(), request.format()
        );
    }
}

record DataExportRequest(@NotNull UUID requestId, String format) { }
