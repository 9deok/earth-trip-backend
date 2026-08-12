package com.earthtrip.identity.application.service.export;

import com.earthtrip.identity.application.port.in.DataExportDownloadUseCase;
import com.earthtrip.identity.application.port.out.DataExportStorePort;
import com.earthtrip.identity.application.port.out.PersonalDataExporterPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DataExportDownloadService implements DataExportDownloadUseCase {

    private final DataExportStorePort store;
    private final PersonalDataExporterPort exporter;
    private final Clock clock;

    DataExportDownloadService(
            DataExportStorePort store, PersonalDataExporterPort exporter, Clock clock) {
        this.store = store;
        this.exporter = exporter;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadResult download(UUID actorUserId, UUID exportId) {
        DataExportStorePort.ExportRecord record =
                store.findById(exportId)
                        .filter(export -> export.userId().equals(actorUserId))
                        .filter(export -> "READY".equals(export.status()))
                        .orElseThrow(
                                () ->
                                        EarthTripException.notFound(
                                                "DATA_EXPORT_NOT_FOUND",
                                                "다운로드할 개인정보 내보내기를 찾을 수 없습니다."));
        if (record.expiresAt() == null || !record.expiresAt().isAfter(clock.instant())) {
            throw EarthTripException.notFound("DATA_EXPORT_EXPIRED", "개인정보 내보내기 다운로드 기간이 만료되었습니다.");
        }
        PersonalDataExporterPort.DownloadArtifact artifact =
                exporter.download(actorUserId, exportId, record.format());
        return new DownloadResult(artifact.content(), artifact.contentType(), artifact.fileName());
    }
}
