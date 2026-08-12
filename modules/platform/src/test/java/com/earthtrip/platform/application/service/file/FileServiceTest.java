package com.earthtrip.platform.application.service.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.platform.application.port.out.FileStorePort;
import com.earthtrip.platform.application.port.out.MalwareScannerPort;
import com.earthtrip.platform.application.port.out.ObjectStoragePort;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class FileServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void 파일_메타데이터_저장이_충돌하면_실제_객체를_먼저_삭제하지_않는다() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        String storageKey = "users/" + userId + "/files/" + fileId;
        FileStorePort.FileRecord file =
                new FileStorePort.FileRecord(
                        fileId,
                        userId,
                        "ticket.pdf",
                        "application/pdf",
                        100,
                        "a".repeat(64),
                        storageKey,
                        "READY",
                        NOW,
                        NOW,
                        null,
                        3);
        FileStorePort store = mock(FileStorePort.class);
        ObjectStoragePort objects = mock(ObjectStoragePort.class);
        when(store.file(fileId)).thenReturn(Optional.of(file));
        when(store.links(fileId)).thenReturn(List.of());
        when(store.saveFile(org.mockito.ArgumentMatchers.any()))
                .thenThrow(
                        new ObjectOptimisticLockingFailureException(
                                FileStorePort.FileRecord.class, fileId));
        FileService service =
                new FileService(
                        store,
                        objects,
                        mock(MalwareScannerPort.class),
                        mock(TripAccess.class),
                        Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.delete(userId, fileId, 3))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(objects, never()).delete(storageKey);
    }
}
