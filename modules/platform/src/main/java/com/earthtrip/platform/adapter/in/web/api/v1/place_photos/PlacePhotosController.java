package com.earthtrip.platform.adapter.in.web.api.v1.place_photos;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/place-photos")
class PlacePhotosController {

    private final ProviderProxyUseCase useCase;

    PlacePhotosController(ProviderProxyUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    ResponseEntity<byte[]> get(
            @RequestParam String name,
            @RequestParam(required = false) Integer maxWidth,
            @RequestParam(required = false) Integer maxHeight) {
        ProviderProxyUseCase.PlacePhoto photo = useCase.placePhoto(name, maxWidth, maxHeight);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(photo.bytes());
    }
}
