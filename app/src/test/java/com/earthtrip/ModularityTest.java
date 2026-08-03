package com.earthtrip;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void 모듈_간_순환과_내부_참조가_없다() {
        ApplicationModules.of(EarthTripApplication.class).verify();
    }
}
