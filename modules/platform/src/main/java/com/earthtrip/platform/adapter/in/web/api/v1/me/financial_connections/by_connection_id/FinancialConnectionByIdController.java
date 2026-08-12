package com.earthtrip.platform.adapter.in.web.api.v1.me.financial_connections.by_connection_id;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.sharedkernel.security.CurrentActor;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/financial-connections/{connectionId}")
class FinancialConnectionByIdController {
    private final IntegrationUseCase u;
    private final CurrentActor a;

    FinancialConnectionByIdController(IntegrationUseCase u, CurrentActor a) {
        this.u = u;
        this.a = a;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID connectionId, @RequestParam @PositiveOrZero long baseVersion) {
        u.deleteConnection(a.requireUserId(), connectionId, "FINANCIAL", baseVersion);
    }
}
