package com.foodgram.repository;

import com.foodgram.domain.Favorite;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    @Modifying
    @Query("delete from Favorite f where f.user.id = :userId and f.recipe.id = :recipeId")
    int deleteByUserAndRecipe(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Query("select f.recipe.id from Favorite f where f.user.id = :userId and f.recipe.id in :recipeIds")
    List<Long> findRecipeIds(@Param("userId") Long userId, @Param("recipeIds") Collection<Long> recipeIds);
}
