package com.foodgram.repository;

import com.foodgram.domain.ShoppingCartItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Long> {

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("delete from ShoppingCartItem c where c.user.id = :userId and c.recipe.id = :recipeId")
    int deleteByUserAndRecipe(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Query("select c.recipe.id from ShoppingCartItem c where c.user.id = :userId and c.recipe.id in :recipeIds")
    List<Long> findRecipeIds(@Param("userId") Long userId, @Param("recipeIds") Collection<Long> recipeIds);
}
