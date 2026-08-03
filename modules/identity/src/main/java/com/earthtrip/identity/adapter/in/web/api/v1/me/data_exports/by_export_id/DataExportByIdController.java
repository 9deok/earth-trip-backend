package com.earthtrip.identity.adapter.in.web.api.v1.me.data_exports.by_export_id;

import com.earthtrip.identity.application.port.in.CurrentUserProvider;
import com.earthtrip.identity.application.port.in.DataExportUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/data-exports/{exportId}")
class DataExportByIdController {

    private final DataExportUseCase useCase;
    private final CurrentUserProvider currentUser;

    DataExportByIdController(DataExportUseCase useCase, CurrentUserProvider currentUser) {
        this.useCase = useCase;
        this.currentUser = currentUser;
    }

    @GetMapping
    DataExportUseCase.ExportResult get(@PathVariable UUID exportId) {
        return useCase.get(currentUser.requireUserId(), exportId);
    }
}
