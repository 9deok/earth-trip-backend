package com.earthtrip.platform.application.service.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.notification.api.PushDeliveryEvents;
import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.platform.application.port.out.OperationalStorePort;
import com.earthtrip.platform.application.port.out.WebhookSecurityPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InternalOperationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    private final OperationalStorePort store = mock(OperationalStorePort.class);
    private final WebhookSecurityPort security = mock(WebhookSecurityPort.class);
    private final FileStorePort files = mock(FileStorePort.class);
    private final IntegrationStorePort integrations = mock(IntegrationStorePort.class);
    private final PushDeliveryEvents pushEvents = mock(PushDeliveryEvents.class);
    private InternalOperationsService service;

    @BeforeEach
    void setUp() {
        when(store.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.saveWebhookReceipt(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.saveDeadLetter(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.saveAudit(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new InternalOperationsService(
            store,
            security,
            new InternalWebhookProcessor(files, integrations, pushEvents, clock),
            new ObjectMapper().findAndRegisterModules(),
            clock
        );
    }

    @Test
    void 악성코드_검사_성공을_READY로_반영한다() {
        UUID fileId = UUID.randomUUID();
        when(security.verify(any(), any(), any(), any(), any())).thenReturn(
            new WebhookSecurityPort.VerifiedWebhook(
                "malware-scan", "scan-1", "a".repeat(64)
            )
        );
        when(store.webhookReceipt("malware-scan", "scan-1")).thenReturn(Optional.empty());
        when(files.file(fileId)).thenReturn(Optional.of(file(fileId, "SCANNING")));
        when(files.saveFile(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.acceptWebhook(
            "malware-scan",
            "scan-1",
            "unused-by-mock",
            "unused-by-mock",
            "{\"fileId\":\"" + fileId + "\",\"result\":\"SAFE\"}"
        );

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        ArgumentCaptor<FileStorePort.FileRecord> saved = ArgumentCaptor.forClass(
            FileStorePort.FileRecord.class
        );
        verify(files).saveFile(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("READY");
    }

    @Test
    void 처리_실패를_작업과_데드레터에_남긴다() {
        UUID missingFileId = UUID.randomUUID();
        when(security.verify(any(), any(), any(), any(), any())).thenReturn(
            new WebhookSecurityPort.VerifiedWebhook(
                "malware-scan", "scan-2", "b".repeat(64)
            )
        );
        when(store.webhookReceipt("malware-scan", "scan-2")).thenReturn(Optional.empty());
        when(files.file(missingFileId)).thenReturn(Optional.empty());
        when(store.openDeadLetterForJob(any())).thenReturn(Optional.empty());

        var result = service.acceptWebhook(
            "malware-scan",
            "scan-2",
            "unused-by-mock",
            "unused-by-mock",
            "{\"fileId\":\"" + missingFileId + "\",\"result\":\"SAFE\"}"
        );

        assertThat(result.status()).isEqualTo("FAILED");
        ArgumentCaptor<OperationalStorePort.DeadLetterRecord> deadLetter =
            ArgumentCaptor.forClass(OperationalStorePort.DeadLetterRecord.class);
        verify(store).saveDeadLetter(deadLetter.capture());
        assertThat(deadLetter.getValue().errorCode()).isEqualTo("FILE_NOT_FOUND");
        assertThat(deadLetter.getValue().status()).isEqualTo("OPEN");
    }

    private static FileStorePort.FileRecord file(UUID id, String status) {
        return new FileStorePort.FileRecord(
            id,
            UUID.randomUUID(),
            "ticket.pdf",
            "application/pdf",
            10,
            "c".repeat(64),
            "files/" + id,
            status,
            NOW.minusSeconds(60),
            NOW.minusSeconds(30),
            null,
            1
        );
    }
}
