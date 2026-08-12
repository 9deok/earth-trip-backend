package com.earthtrip.platform.application.service.info;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlatformInfoServiceTest {

    @Test
    void 통화가_없는_국가도_국가_목록에_안전하게_포함한다() {
        PlatformInfoService service = service();

        var antarctica =
                service.countries().stream()
                        .filter(country -> "AQ".equals(country.code()))
                        .findFirst()
                        .orElseThrow();

        assertThat(antarctica.displayName()).isNotBlank();
        assertThat(antarctica.currencyCode()).isNull();
    }

    private static PlatformInfoService service() {
        return new PlatformInfoService(
                1,
                1,
                false,
                "",
                true,
                "ap-northeast-2",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "");
    }
}
