package com.earthtrip.wallet.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class AesSensitiveWalletDataAdapterTest {

    private static final String OLD_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String NEW_KEY = Base64.getEncoder().encodeToString(bytes(7));
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Test
    void 민감값을_AES_GCM으로_암호화하고_원래_자료형으로_복호화한다() {
        AesSensitiveWalletDataAdapter adapter =
                new AesSensitiveWalletDataAdapter("primary:" + OLD_KEY, "primary", JSON);

        Object protectedValue = adapter.protect("passengerNames", List.of("홍길동", "김여행"));

        assertThat(adapter.isProtected(protectedValue)).isTrue();
        assertThat(protectedValue.toString()).doesNotContain("홍길동", "김여행");
        assertThat(adapter.reveal("passengerNames", protectedValue))
                .isEqualTo(List.of("홍길동", "김여행"));
    }

    @Test
    void 이전_key를_key_ring에_유지하면_rotation_후에도_복호화한다() {
        AesSensitiveWalletDataAdapter oldAdapter =
                new AesSensitiveWalletDataAdapter("old:" + OLD_KEY, "old", JSON);
        Object protectedValue = oldAdapter.protect("confirmationNumber", "ABC-1234");
        AesSensitiveWalletDataAdapter rotated =
                new AesSensitiveWalletDataAdapter(
                        "primary:" + NEW_KEY + ",old:" + OLD_KEY, "primary", JSON);

        assertThat(rotated.reveal("confirmationNumber", protectedValue)).isEqualTo("ABC-1234");
    }

    @Test
    void key가_없으면_민감값을_평문으로_저장하지_않는다() {
        AesSensitiveWalletDataAdapter adapter =
                new AesSensitiveWalletDataAdapter("", "primary", JSON);

        assertThatThrownBy(() -> adapter.protect("confirmationNumber", "ABC-1234"))
                .isInstanceOfSatisfying(
                        EarthTripException.class,
                        exception -> {
                            assertThat(exception.code())
                                    .isEqualTo("WALLET_ENCRYPTION_NOT_CONFIGURED");
                            assertThat(exception.httpStatus()).isEqualTo(503);
                        });
    }

    private static byte[] bytes(int value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) value);
        return bytes;
    }
}
