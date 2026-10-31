package com.foodgram.service;

import com.foodgram.domain.Ingredient;
import com.foodgram.repository.IngredientRepository;
import com.foodgram.web.dto.IngredientDto;
import com.foodgram.web.error.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<IngredientDto> list(String namePrefix) {
        List<Ingredient> found = (namePrefix == null || namePrefix.isEmpty())
                ? ingredientRepository.findAllByOrderByNameAsc()
                : ingredientRepository.findByNameStartingWithIgnoreCaseOrderByNameAsc(namePrefix);
        return found.stream().map(IngredientService::toDto).toList();
    }

    public IngredientDto get(Long id) {
        return ingredientRepository.findById(id).map(IngredientService::toDto)
                .orElseThrow(ApiException::notFound);
    }

    private static IngredientDto toDto(Ingredient i) {
        return new IngredientDto(i.getId(), i.getName(), i.getMeasurementUnit());
    }
}
