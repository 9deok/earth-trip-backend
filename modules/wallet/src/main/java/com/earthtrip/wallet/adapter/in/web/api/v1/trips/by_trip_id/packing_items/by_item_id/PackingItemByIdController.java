package com.earthtrip.wallet.adapter.in.web.api.v1.trips.by_trip_id.packing_items.by_item_id;

import com.earthtrip.sharedkernel.security.CurrentActor;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/packing-items/{itemId}")
class PackingItemByIdController {
    private final WalletRecordUseCase useCase;
    private final CurrentActor actor;

    PackingItemByIdController(WalletRecordUseCase u, CurrentActor a) {
        useCase = u;
        actor = a;
    }

    @PatchMapping
    WalletRecordUseCase.RecordResult patch(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PackingMutation r) {
        return useCase.update(
                tripId,
                actor.requireUserId(),
                "PACKING_ITEM",
                itemId,
                true,
                new WalletRecordUseCase.Command(
                        itemId,
                        null,
                        r.payload(),
                        r.status(),
                        r.visibility(),
                        r.sortOrder(),
                        r.baseVersion()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PackingDelete r) {
        useCase.delete(
                tripId, actor.requireUserId(), "PACKING_ITEM", itemId, true, r.baseVersion());
    }
}

record PackingMutation(
        Map<String, Object> payload,
        String status,
        String visibility,
        Integer sortOrder,
        @Min(0) long baseVersion) {}

record PackingDelete(@Min(0) long baseVersion) {}
