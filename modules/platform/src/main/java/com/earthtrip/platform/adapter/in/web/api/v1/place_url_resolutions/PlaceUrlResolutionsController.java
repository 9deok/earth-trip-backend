package com.earthtrip.platform.adapter.in.web.api.v1.place_url_resolutions;
import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/place-url-resolutions")class PlaceUrlResolutionsController{private final ExternalTravelUseCase u;PlaceUrlResolutionsController(ExternalTravelUseCase u){this.u=u;}@PostMapping ExternalTravelUseCase.PlaceUrlResult post(@Valid @RequestBody PlaceUrlRequest r){return u.resolvePlaceUrl(r.url(),r.language());}}
record PlaceUrlRequest(@NotBlank @Size(max=2048)String url,String language){}
