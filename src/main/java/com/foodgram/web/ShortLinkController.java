package com.foodgram.web;

import com.foodgram.service.RecipeService;
import com.foodgram.service.ShortLinkService;
import com.foodgram.web.error.ApiException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final RecipeService recipeService;

    public ShortLinkController(ShortLinkService shortLinkService, RecipeService recipeService) {
        this.shortLinkService = shortLinkService;
        this.recipeService = recipeService;
    }

    @GetMapping("/s/{hash}")
    public ResponseEntity<Void> redirect(@PathVariable String hash) {
        Long recipeId = shortLinkService.decode(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Короткая ссылка повреждена"));
        recipeService.requireExists(recipeId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create("/recipes/" + recipeId + "/").toString())
                .build();
    }
}
