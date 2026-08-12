package com.earthtrip.wallet.application.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PackingTemplateServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void 빈_템플릿을_만든_뒤_나중에_항목을_채울_수_있다() {
        TripAccess access = mock(TripAccess.class);
        WalletRecordUseCase wallet = mock(WalletRecordUseCase.class);
        PackingTemplateStorePort store = mock(PackingTemplateStorePort.class);
        UUID actor = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(store.findById(requestId)).thenReturn(Optional.empty());
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PackingTemplateService service = service(access, wallet, store);

        var result =
                service.create(
                        actor,
                        new PackingTemplateUseCase.TemplateCommand(
                                requestId, "겨울 여행", "PERSONAL", List.of(), 0));

        assertThat(result.items()).isEmpty();
        assertThat(result.name()).isEqualTo("겨울 여행");
    }

    @Test
    void 템플릿_적용은_이미_있는_준비물을_중복으로_만들지_않는다() {
        TripAccess access = mock(TripAccess.class);
        WalletRecordUseCase wallet = mock(WalletRecordUseCase.class);
        PackingTemplateStorePort store = mock(PackingTemplateStorePort.class);
        UUID actor = UUID.randomUUID();
        UUID trip = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        var template =
                new PackingTemplateStorePort.TemplateRecord(
                        templateId,
                        actor,
                        "해외여행",
                        "PERSONAL",
                        List.of(
                                new PackingTemplateUseCase.TemplateItem("여권", "문서", 1, null),
                                new PackingTemplateUseCase.TemplateItem("충전기", "전자기기", 1, null)),
                        NOW,
                        NOW,
                        null,
                        0);
        when(access.requireEditor(trip, actor))
                .thenReturn(new TripAccess.AccessResult(trip, actor, "OWNER", 0));
        when(store.findApplication(applicationId)).thenReturn(Optional.empty());
        when(store.findById(templateId)).thenReturn(Optional.of(template));
        when(wallet.list(trip, actor, "PACKING_ITEM", null))
                .thenReturn(List.of(record(trip, actor, "여권")));
        when(store.saveApplication(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PackingTemplateService service = service(access, wallet, store);

        var result =
                service.apply(
                        trip,
                        actor,
                        new PackingTemplateUseCase.ApplicationCommand(
                                applicationId, templateId, "TRIP"));

        assertThat(result.packingItemIds()).hasSize(1);
        ArgumentCaptor<WalletRecordUseCase.Command> command =
                ArgumentCaptor.forClass(WalletRecordUseCase.Command.class);
        verify(wallet, times(1))
                .create(eq(trip), eq(actor), eq("PACKING_ITEM"), eq(true), command.capture());
        assertThat(command.getValue().payload().get("name")).isEqualTo("충전기");
    }

    private static PackingTemplateService service(
            TripAccess access, WalletRecordUseCase wallet, PackingTemplateStorePort store) {
        return new PackingTemplateService(access, wallet, store, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static WalletRecordUseCase.RecordResult record(UUID trip, UUID actor, String name) {
        return new WalletRecordUseCase.RecordResult(
                UUID.randomUUID(),
                trip,
                "PACKING_ITEM",
                null,
                Map.of("name", name),
                "OPEN",
                "TRIP",
                0,
                0,
                actor,
                actor,
                NOW,
                NOW);
    }
}
