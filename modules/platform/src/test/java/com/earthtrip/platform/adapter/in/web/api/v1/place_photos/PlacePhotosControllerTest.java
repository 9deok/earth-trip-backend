package com.earthtrip.platform.adapter.in.web.api.v1.place_photos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PlacePhotosControllerTest {

    @Test
    void Google_장소_사진은_중간_캐시에_저장하지_않는다() {
        ProviderProxyUseCase useCase = mock(ProviderProxyUseCase.class);
        when(useCase.placePhoto("places/tokyo/photos/main", 480, null))
                .thenReturn(
                        new ProviderProxyUseCase.PlacePhoto(new byte[] {1, 2, 3}, "image/jpeg"));
        PlacePhotosController controller = new PlacePhotosController(useCase);

        ResponseEntity<byte[]> response = controller.get("places/tokyo/photos/main", 480, null);

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }
}
