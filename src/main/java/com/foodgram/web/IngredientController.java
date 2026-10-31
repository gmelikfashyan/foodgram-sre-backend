package com.foodgram.web;

import com.foodgram.service.IngredientService;
import com.foodgram.web.dto.IngredientDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredientDto> list(@RequestParam(required = false) String name) {
        return ingredientService.list(name);
    }

    @GetMapping("/{id}")
    public IngredientDto get(@PathVariable Long id) {
        return ingredientService.get(id);
    }
}
