package com.earthtrip.platform.application.service.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import org.junit.jupiter.api.Test;

class FilePolicyTest {

    @Test
    void 허용된_파일_메타데이터를_정규화한다() {
        assertThat(FilePolicy.fileName("  ticket.pdf  ")).isEqualTo("ticket.pdf");
        assertThat(FilePolicy.mimeType("APPLICATION/PDF")).isEqualTo("application/pdf");
        assertThat(FilePolicy.resourceType("reservation")).isEqualTo("RESERVATION");
        assertThat(FilePolicy.resourceType("support_request")).isEqualTo("SUPPORT_REQUEST");
        assertThat(FilePolicy.visibility(null)).isEqualTo("TRIP");
    }

    @Test
    void 경로가_포함된_파일명과_과대_파일을_거부한다() {
        assertThatThrownBy(() -> FilePolicy.fileName("../secret.txt"))
                .isInstanceOf(EarthTripException.class);
        assertThatThrownBy(() -> FilePolicy.size(25L * 1024L * 1024L + 1))
                .isInstanceOf(EarthTripException.class);
    }
}
