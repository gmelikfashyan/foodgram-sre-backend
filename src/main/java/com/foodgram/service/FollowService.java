package com.foodgram.service;

import com.foodgram.domain.Follow;
import com.foodgram.domain.Recipe;
import com.foodgram.domain.User;
import com.foodgram.repository.FollowRepository;
import com.foodgram.repository.RecipeRepository;
import com.foodgram.repository.UserRepository;
import com.foodgram.web.dto.RecipeShortDto;
import com.foodgram.web.dto.UserWithRecipesDto;
import com.foodgram.web.error.ApiException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository,
                         RecipeRepository recipeRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserWithRecipesDto> subscriptions(User viewer, int page, int limit, Integer recipesLimit) {
        Page<Follow> follows = followRepository.findByUserId(viewer.getId(),
                PageRequest.of(page - 1, limit,
                        Sort.by("author.lastName", "author.firstName", "id")));
        return follows.map(f -> toDto(f.getAuthor(), true, recipesLimit));
    }

    @Transactional
    public UserWithRecipesDto subscribe(User viewer, Long authorId, Integer recipesLimit) {
        User author = userRepository.findById(authorId).orElseThrow(ApiException::notFound);
        if (author.getId().equals(viewer.getId())) {
            throw ApiException.badRequest("Невозможно подписаться на самого себя.");
        }
        if (followRepository.existsByUserIdAndAuthorId(viewer.getId(), authorId)) {
            throw ApiException.badRequest("Вы уже подписаны на этого пользователя.");
        }
        followRepository.save(new Follow(userRepository.getReferenceById(viewer.getId()), author));
        return toDto(author, true, recipesLimit);
    }

    @Transactional
    public void unsubscribe(User viewer, Long authorId) {
        if (!userRepository.existsById(authorId)) {
            throw ApiException.notFound();
        }
        if (followRepository.deleteByUserAndAuthor(viewer.getId(), authorId) == 0) {
            throw ApiException.badRequest("Вы не подписаны на пользователя.");
        }
    }

    private UserWithRecipesDto toDto(User author, boolean subscribed, Integer recipesLimit) {
        List<Recipe> recipes = (recipesLimit != null)
                ? (recipesLimit == 0
                        ? List.of()
                        : recipeRepository.findByAuthorIdOrderByIdDesc(author.getId(),
                                PageRequest.of(0, recipesLimit)))
                : recipeRepository.findAllByAuthorIdOrderByIdDesc(author.getId());
        List<RecipeShortDto> shorts = recipes.stream()
                .map(r -> new RecipeShortDto(r.getId(), r.getName(), r.getImage(), r.getCookingTime()))
                .toList();
        return new UserWithRecipesDto(author.getEmail(), author.getId(), author.getUsername(),
                author.getFirstName(), author.getLastName(), subscribed, shorts,
                recipeRepository.countByAuthorId(author.getId()), author.getAvatar());
    }
}
