package com.earthtrip.identity.adapter.out.export;

import com.earthtrip.identity.application.port.out.PersonalDataExporterPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UnconfiguredPersonalDataExporter implements PersonalDataExporterPort {

    @Override
    public ExportArtifact export(UUID userId, UUID exportId, String format) {
        throw EarthTripException.unavailable(
            "DATA_EXPORT_STORAGE_NOT_CONFIGURED",
            "개인정보 내보내기 저장소가 설정되지 않았습니다."
        );
    }
}
