package com.foodgram.web;

import com.foodgram.domain.User;
import com.foodgram.service.RecipeService;
import com.foodgram.web.dto.PageDto;
import com.foodgram.web.dto.RecipeDto;
import com.foodgram.web.dto.RecipeShortDto;
import com.foodgram.web.dto.RecipeWriteRequest;
import com.foodgram.web.dto.ShortLinkDto;
import com.foodgram.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final ShortLinkService shortLinkService;
    private final Paging paging;

    public RecipeController(RecipeService recipeService, ShortLinkService shortLinkService, Paging paging) {
        this.recipeService = recipeService;
        this.shortLinkService = shortLinkService;
        this.paging = paging;
    }

    @GetMapping
    public PageDto<RecipeDto> list(@AuthenticationPrincipal User viewer,
                                   @RequestParam(required = false) Long author,
                                   @RequestParam(name = "is_favorited", required = false) String isFavorited,
                                   @RequestParam(name = "is_in_shopping_cart", required = false) String isInCart,
                                   @RequestParam(required = false) String page,
                                   @RequestParam(required = false) String limit,
                                   HttpServletRequest request) {
        int pageNumber = paging.page(page);
        Page<RecipeDto> result = recipeService.list(viewer, author, Paging.flag(isFavorited),
                Paging.flag(isInCart), pageNumber, paging.limit(limit));
        return paging.toDto(result, pageNumber, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDto create(@AuthenticationPrincipal User viewer, @RequestBody RecipeWriteRequest body) {
        return recipeService.create(viewer, body);
    }

    @GetMapping("/{id}")
    public RecipeDto get(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        return recipeService.get(viewer, id);
    }

    @PatchMapping("/{id}")
    public RecipeDto patch(@PathVariable Long id, @AuthenticationPrincipal User viewer,
                           @RequestBody RecipeWriteRequest body) {
        return recipeService.update(viewer, id, body);
    }

    @PutMapping("/{id}")
    public RecipeDto put(@PathVariable Long id, @AuthenticationPrincipal User viewer,
                         @RequestBody RecipeWriteRequest body) {
        return recipeService.update(viewer, id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        recipeService.delete(viewer, id);
    }

    @PostMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeShortDto addFavorite(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        return recipeService.addFavorite(viewer, id);
    }

    @DeleteMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        recipeService.removeFavorite(viewer, id);
    }

    @PostMapping("/{id}/shopping_cart")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeShortDto addToCart(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        return recipeService.addToCart(viewer, id);
    }

    @DeleteMapping("/{id}/shopping_cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromCart(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        recipeService.removeFromCart(viewer, id);
    }

    @GetMapping("/download_shopping_cart")
    public ResponseEntity<String> downloadShoppingCart(@AuthenticationPrincipal User viewer) {
        String body = recipeService.shoppingListText(viewer);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"wishList.txt\"")
                .body(body);
    }

    /** Ссылка на страницу рецепта во фронтенде: http://host/recipes/{id}/ */
    @GetMapping("/{id}/get-link")
    public ShortLinkDto getLink(@PathVariable Long id, HttpServletRequest request) {
        recipeService.requireExists(id);
        return new ShortLinkDto(base(request) + "/recipes/" + id + "/");
    }

    /** Короткая ссылка: http://host/s/{hash}/ */
    @GetMapping("/{id}/get-short-link")
    public ShortLinkDto getShortLink(@PathVariable Long id, HttpServletRequest request) {
        recipeService.requireExists(id);
        return new ShortLinkDto(base(request) + "/s/" + shortLinkService.encode(id) + "/");
    }

    private static String base(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }
}
