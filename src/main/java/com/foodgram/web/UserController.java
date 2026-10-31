package com.foodgram.web;

import com.foodgram.domain.User;
import com.foodgram.service.FollowService;
import com.foodgram.service.UserMapper;
import com.foodgram.service.UserService;
import com.foodgram.web.dto.AvatarRequest;
import com.foodgram.web.dto.AvatarResponse;
import com.foodgram.web.dto.PageDto;
import com.foodgram.web.dto.SetPasswordRequest;
import com.foodgram.web.dto.UserCreateRequest;
import com.foodgram.web.dto.UserCreateResponse;
import com.foodgram.web.dto.UserDto;
import com.foodgram.web.dto.UserWithRecipesDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final UserMapper userMapper;
    private final Paging paging;

    public UserController(UserService userService, FollowService followService,
                          UserMapper userMapper, Paging paging) {
        this.userService = userService;
        this.followService = followService;
        this.userMapper = userMapper;
        this.paging = paging;
    }

    @GetMapping
    public PageDto<UserDto> list(@AuthenticationPrincipal User viewer,
                                 @RequestParam(required = false) String page,
                                 @RequestParam(required = false) String limit,
                                 HttpServletRequest request) {
        int pageNumber = paging.page(page);
        Page<User> users = userService.list(pageNumber, paging.limit(limit));
        return paging.toDto(users, list -> userMapper.toDtos(list, viewer), pageNumber, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreateResponse create(@RequestBody UserCreateRequest body) {
        User u = userService.register(body);
        return new UserCreateResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User viewer) {
        return userMapper.toDto(userService.get(viewer.getId()), false);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        return userMapper.toDto(userService.get(id), viewer);
    }

    @PutMapping("/me/avatar")
    public AvatarResponse setAvatar(@AuthenticationPrincipal User viewer, @RequestBody AvatarRequest body) {
        return new AvatarResponse(userService.setAvatar(viewer, body.avatar()));
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(@AuthenticationPrincipal User viewer) {
        userService.deleteAvatar(viewer);
    }

    @GetMapping("/subscriptions")
    public PageDto<UserWithRecipesDto> subscriptions(@AuthenticationPrincipal User viewer,
                                                     @RequestParam(required = false) String page,
                                                     @RequestParam(required = false) String limit,
                                                     @RequestParam(name = "recipes_limit", required = false)
                                                     String recipesLimit,
                                                     HttpServletRequest request) {
        int pageNumber = paging.page(page);
        Page<UserWithRecipesDto> result = followService.subscriptions(
                viewer, pageNumber, paging.limit(limit), parseRecipesLimit(recipesLimit));
        return paging.toDto(result, pageNumber, request);
    }

    @PostMapping("/{id}/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public UserWithRecipesDto subscribe(@PathVariable Long id, @AuthenticationPrincipal User viewer,
                                        @RequestParam(name = "recipes_limit", required = false)
                                        String recipesLimit) {
        return followService.subscribe(viewer, id, parseRecipesLimit(recipesLimit));
    }

    @DeleteMapping("/{id}/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        followService.unsubscribe(viewer, id);
    }

    @PostMapping("/set_password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@AuthenticationPrincipal User viewer, @RequestBody SetPasswordRequest body) {
        userService.setPassword(viewer, body);
    }

    /** Некорректное или отрицательное значение игнорируется (как в исходной реализации). */
    private static Integer parseRecipesLimit(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
