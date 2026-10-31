package com.foodgram.repository;

import com.foodgram.domain.RecipeIngredient;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @Query("select ri from RecipeIngredient ri join fetch ri.ingredient "
            + "where ri.recipe.id in :recipeIds order by ri.id")
    List<RecipeIngredient> findAllByRecipeIds(@Param("recipeIds") Collection<Long> recipeIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RecipeIngredient ri where ri.recipe.id = :recipeId")
    void deleteAllByRecipeId(@Param("recipeId") Long recipeId);

    /** Суммарное количество каждого ингредиента по рецептам из корзины пользователя. */
    @Query("select i.name, i.measurementUnit, sum(ri.amount) "
            + "from RecipeIngredient ri join ri.ingredient i "
            + "where ri.recipe.id in (select c.recipe.id from ShoppingCartItem c where c.user.id = :userId) "
            + "group by i.id, i.name, i.measurementUnit "
            + "order by i.name")
    List<Object[]> sumForShoppingCart(@Param("userId") Long userId);
}
