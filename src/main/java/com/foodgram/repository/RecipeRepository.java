package com.foodgram.repository;

import com.foodgram.domain.Recipe;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);

    long countByAuthorId(Long authorId);

    List<Recipe> findByAuthorIdOrderByIdDesc(Long authorId, Pageable pageable);

    List<Recipe> findAllByAuthorIdOrderByIdDesc(Long authorId);
}
