package com.foodgram.service;

import com.foodgram.domain.Favorite;
import com.foodgram.domain.Ingredient;
import com.foodgram.domain.Recipe;
import com.foodgram.domain.RecipeIngredient;
import com.foodgram.domain.ShoppingCartItem;
import com.foodgram.domain.User;
import com.foodgram.repository.FavoriteRepository;
import com.foodgram.repository.IngredientRepository;
import com.foodgram.repository.RecipeIngredientRepository;
import com.foodgram.repository.RecipeRepository;
import com.foodgram.repository.ShoppingCartRepository;
import com.foodgram.repository.UserRepository;
import com.foodgram.web.dto.IngredientAmountRequest;
import com.foodgram.web.dto.RecipeDto;
import com.foodgram.web.dto.RecipeIngredientDto;
import com.foodgram.web.dto.RecipeShortDto;
import com.foodgram.web.dto.RecipeWriteRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.foodgram.web.error.ApiException;
import com.foodgram.web.error.FieldErrors;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    static final int MIN_AMOUNT = 1;
    static final int MAX_AMOUNT = 32_000;
    static final int MIN_COOKING_TIME = 1;
    static final int MAX_COOKING_TIME = 32_000;

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final FavoriteRepository favoriteRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ImageStorage imageStorage;

    public RecipeService(RecipeRepository recipeRepository,
                         RecipeIngredientRepository recipeIngredientRepository,
                         IngredientRepository ingredientRepository,
                         FavoriteRepository favoriteRepository,
                         ShoppingCartRepository shoppingCartRepository,
                         UserRepository userRepository,
                         UserMapper userMapper,
                         ImageStorage imageStorage) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.imageStorage = imageStorage;
    }

    // ---------------------------------------------------------------- чтение

    @Transactional(readOnly = true)
    public Page<RecipeDto> list(User viewer, Long authorId, boolean onlyFavorited,
                                boolean onlyInCart, int page, int limit) {
        Long viewerId = viewer == null ? null : viewer.getId();

        Specification<Recipe> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }
            if (onlyFavorited) {
                if (viewerId == null) {
                    predicates.add(cb.disjunction());
                } else {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Favorite> f = sq.from(Favorite.class);
                    sq.select(f.<Long>get("id")).where(
                            cb.equal(f.get("recipe"), root),
                            cb.equal(f.get("user").get("id"), viewerId));
                    predicates.add(cb.exists(sq));
                }
            }
            if (onlyInCart) {
                if (viewerId == null) {
                    predicates.add(cb.disjunction());
                } else {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<ShoppingCartItem> c = sq.from(ShoppingCartItem.class);
                    sq.select(c.<Long>get("id")).where(
                            cb.equal(c.get("recipe"), root),
                            cb.equal(c.get("user").get("id"), viewerId));
                    predicates.add(cb.exists(sq));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Recipe> recipes = recipeRepository.findAll(spec,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "id")));
        List<RecipeDto> dtos = toDtos(recipes.getContent(), viewer);
        return new PageImpl<>(dtos, recipes.getPageable(),
                recipes.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RecipeDto get(User viewer, Long id) {
        return toDtos(List.of(getOr404(id)), viewer).get(0);
    }

    // ---------------------------------------------------------------- запись

    @Transactional
    public RecipeDto create(User viewer, RecipeWriteRequest r) {
        Prepared p = validate(r, true);
        User author = userRepository.getReferenceById(viewer.getId());
        String imageUrl = imageStorage.save("recipes/images", p.image());
        Recipe recipe = recipeRepository.save(
                new Recipe(author, r.name().trim(), imageUrl, r.text(), p.cookingTime()));
        saveIngredients(recipe, p.ingredients());
        return toDtos(List.of(recipe), viewer).get(0);
    }

    @Transactional
    public RecipeDto update(User viewer, Long id, RecipeWriteRequest r) {
        Recipe recipe = getOr404(id);
        requireAuthor(viewer, recipe);
        Prepared p = validate(r, false);

        if (r.name() != null) {
            recipe.setName(r.name().trim());
        }
        if (r.text() != null) {
            recipe.setText(r.text());
        }
        if (p.cookingTime() != null) {
            recipe.setCookingTime(p.cookingTime());
        }
        if (p.image() != null) {
            String old = recipe.getImage();
            recipe.setImage(imageStorage.save("recipes/images", p.image()));
            imageStorage.delete(old);
        }
        // flush изменений рецепта + удаление старых ингредиентов; контекст персистентности очищается
        recipeIngredientRepository.deleteAllByRecipeId(recipe.getId());
        Recipe fresh = getOr404(id);
        saveIngredients(fresh, p.ingredients());
        return toDtos(List.of(fresh), viewer).get(0);
    }

    @Transactional
    public void delete(User viewer, Long id) {
        Recipe recipe = getOr404(id);
        requireAuthor(viewer, recipe);
        String image = recipe.getImage();
        recipeRepository.delete(recipe);
        imageStorage.delete(image);
    }

    // ---------------------------------------------------- избранное и корзина

    @Transactional
    public RecipeShortDto addFavorite(User viewer, Long recipeId) {
        Recipe recipe = getOr404(recipeId);
        if (favoriteRepository.existsByUserIdAndRecipeId(viewer.getId(), recipeId)) {
            throw ApiException.badRequest("Вы уже добавили этот рецепт в избранное.");
        }
        favoriteRepository.save(new Favorite(userRepository.getReferenceById(viewer.getId()), recipe));
        return toShort(recipe);
    }

    @Transactional
    public void removeFavorite(User viewer, Long recipeId) {
        getOr404(recipeId);
        if (favoriteRepository.deleteByUserAndRecipe(viewer.getId(), recipeId) == 0) {
            throw ApiException.badRequest("Вы не добавляли этот рецепт в избранное.");
        }
    }

    @Transactional
    public RecipeShortDto addToCart(User viewer, Long recipeId) {
        Recipe recipe = getOr404(recipeId);
        if (shoppingCartRepository.existsByUserIdAndRecipeId(viewer.getId(), recipeId)) {
            throw ApiException.badRequest("Вы уже добавили этот рецепт в список покупок.");
        }
        shoppingCartRepository.save(
                new ShoppingCartItem(userRepository.getReferenceById(viewer.getId()), recipe));
        return toShort(recipe);
    }

    @Transactional
    public void removeFromCart(User viewer, Long recipeId) {
        getOr404(recipeId);
        if (shoppingCartRepository.deleteByUserAndRecipe(viewer.getId(), recipeId) == 0) {
            throw ApiException.badRequest("Вы не добавляли этот рецепт в список покупок.");
        }
    }

    /** Текст файла со списком покупок (суммарно по всем рецептам из корзины). */
    @Transactional(readOnly = true)
    public String shoppingListText(User viewer) {
        if (!shoppingCartRepository.existsByUserId(viewer.getId())) {
            throw ApiException.badRequest("Корзина покупок пуста.");
        }
        StringBuilder sb = new StringBuilder("СПИСОК ПОКУПОК\n").append(" ".repeat(50));
        for (Object[] row : recipeIngredientRepository.sumForShoppingCart(viewer.getId())) {
            sb.append("\n• ").append(row[0]).append(" (").append(row[1]).append(") — ").append(row[2]);
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public void requireExists(Long id) {
        getOr404(id);
    }


    private Recipe getOr404(Long id) {
        return recipeRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    private static void requireAuthor(User viewer, Recipe recipe) {
        if (!recipe.getAuthor().getId().equals(viewer.getId())) {
            throw ApiException.forbidden();
        }
    }

    private void saveIngredients(Recipe recipe, List<IngredientAmountRequest> items) {
        Map<Long, Ingredient> byId = ingredientRepository
                .findAllById(items.stream().map(IngredientAmountRequest::id).toList()).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        List<RecipeIngredient> rows = items.stream()
                .map(i -> new RecipeIngredient(recipe, byId.get(i.id()), i.amount()))
                .toList();
        recipeIngredientRepository.saveAll(rows);
    }

    private static RecipeShortDto toShort(Recipe r) {
        return new RecipeShortDto(r.getId(), r.getName(), r.getImage(), r.getCookingTime());
    }

    private List<RecipeDto> toDtos(List<Recipe> recipes, User viewer) {
        if (recipes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = recipes.stream().map(Recipe::getId).toList();

        Map<Long, List<RecipeIngredientDto>> ingredients = new LinkedHashMap<>();
        for (RecipeIngredient ri : recipeIngredientRepository.findAllByRecipeIds(ids)) {
            ingredients.computeIfAbsent(ri.getRecipe().getId(), k -> new ArrayList<>())
                    .add(new RecipeIngredientDto(ri.getIngredient().getId(), ri.getIngredient().getName(),
                            ri.getIngredient().getMeasurementUnit(), ri.getAmount()));
        }

        Set<Long> favorited = viewer == null ? Set.of()
                : new HashSet<>(favoriteRepository.findRecipeIds(viewer.getId(), ids));
        Set<Long> inCart = viewer == null ? Set.of()
                : new HashSet<>(shoppingCartRepository.findRecipeIds(viewer.getId(), ids));
        Set<Long> subscribed = userMapper.subscribedIds(viewer,
                recipes.stream().map(r -> r.getAuthor().getId()).distinct().toList());

        return recipes.stream().map(r -> new RecipeDto(
                r.getId(),
                userMapper.toDto(r.getAuthor(), subscribed.contains(r.getAuthor().getId())),
                ingredients.getOrDefault(r.getId(), List.of()),
                favorited.contains(r.getId()),
                inCart.contains(r.getId()),
                r.getName(),
                r.getImage(),
                r.getText(),
                r.getCookingTime())).toList();
    }

    private record Prepared(List<IngredientAmountRequest> ingredients, ImageStorage.DecodedImage image,
                            Integer cookingTime) {
    }

    private Prepared validate(RecipeWriteRequest r, boolean create) {
        FieldErrors e = new FieldErrors();

        if (r.name() == null) {
            if (create) {
                e.add("name", FieldErrors.REQUIRED);
            }
        } else if (r.name().isBlank()) {
            e.add("name", "Это поле не может быть пустым.");
        } else if (r.name().length() > 256) {
            e.add("name", "Убедитесь, что это значение содержит не более 256 символов.");
        }

        if (r.text() == null) {
            if (create) {
                e.add("text", FieldErrors.REQUIRED);
            }
        } else if (r.text().isBlank()) {
            e.add("text", "Это поле не может быть пустым.");
        }

        Integer cookingTime = null;
        JsonNode ct = r.cookingTime();
        if (ct == null || ct.isNull() || ct.isMissingNode()) {
            if (create) {
                e.add("cooking_time", FieldErrors.REQUIRED);
            }
        } else if (ct.isTextual() && ct.asText().isBlank()) {
            e.add("cooking_time", "Это поле не может быть пустым.");
        } else if (!(ct.isIntegralNumber() || (ct.isTextual() && ct.asText().trim().matches("-?\\d{1,9}")))) {
            e.add("cooking_time", "Введите целое число.");
        } else {
            int value = ct.isTextual() ? Integer.parseInt(ct.asText().trim()) : ct.asInt();
            if (ct.isIntegralNumber() && !ct.canConvertToInt()) {
                e.add("cooking_time", "Время приготовления не может быть больше " + MAX_COOKING_TIME + " минут");
            } else if (value < MIN_COOKING_TIME) {
                e.add("cooking_time", "Время приготовления не может быть меньше " + MIN_COOKING_TIME + " минуты");
            } else if (value > MAX_COOKING_TIME) {
                e.add("cooking_time", "Время приготовления не может быть больше " + MAX_COOKING_TIME + " минут");
            } else {
                cookingTime = value;
            }
        }

        ImageStorage.DecodedImage image = null;
        if (r.image() == null) {
            if (create) {
                e.add("image", FieldErrors.REQUIRED);
            }
        } else if (r.image().isBlank()) {
            e.add("image", "Это поле не может быть пустым.");
        } else {
            try {
                image = imageStorage.decode(r.image());
            } catch (IllegalArgumentException ex) {
                e.add("image", ex.getMessage());
            }
        }

        List<IngredientAmountRequest> items = r.ingredients();
        if (items == null || items.isEmpty()) {
            e.put("ingredients", List.of(Map.of("non_field_errors", List.of("Ингредиенты не переданы"))));
        } else {
            // Формат как в DRF для вложенных списков: по объекту ошибок на каждый ингредиент ({} если ошибок нет)
            Set<Long> existing = ingredientRepository.findAllById(
                            items.stream().filter(i -> i != null && i.id() != null)
                                    .map(IngredientAmountRequest::id).distinct().toList())
                    .stream().map(Ingredient::getId).collect(Collectors.toSet());
            List<Map<String, List<String>>> itemErrors = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            boolean hasErrors = false;
            for (IngredientAmountRequest item : items) {
                Map<String, List<String>> ie = new LinkedHashMap<>();
                if (item == null || item.id() == null) {
                    ie.put("id", List.of(FieldErrors.REQUIRED));
                } else if (!seen.add(item.id())) {
                    ie.put("id", List.of("Ингредиент не уникален"));
                } else if (!existing.contains(item.id())) {
                    ie.put("id", List.of("Ингредиента с id " + item.id() + " не существует."));
                }
                if (item == null || item.amount() == null) {
                    ie.put("amount", List.of(FieldErrors.REQUIRED));
                } else if (item.amount() < MIN_AMOUNT) {
                    ie.put("amount", List.of("Убедитесь, что это значение больше либо равно " + MIN_AMOUNT + "."));
                } else if (item.amount() > MAX_AMOUNT) {
                    ie.put("amount", List.of("Убедитесь, что это значение меньше либо равно " + MAX_AMOUNT + "."));
                }
                hasErrors |= !ie.isEmpty();
                itemErrors.add(ie);
            }
            if (hasErrors) {
                e.put("ingredients", itemErrors);
            }
        }

        e.throwIfAny();
        return new Prepared(items, image, cookingTime);
    }
}
