package com.foodgram.repository;

import com.foodgram.domain.Ingredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findAllByOrderByNameAsc();

    List<Ingredient> findByNameStartingWithIgnoreCaseOrderByNameAsc(String prefix);
}
