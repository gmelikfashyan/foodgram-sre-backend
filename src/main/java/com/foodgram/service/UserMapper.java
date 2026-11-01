package com.foodgram.service;

import com.foodgram.domain.User;
import com.foodgram.repository.FollowRepository;
import com.foodgram.web.dto.UserDto;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final FollowRepository followRepository;

    public UserMapper(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    public UserDto toDto(User user, boolean subscribed) {
        return new UserDto(user.getEmail(), user.getId(), user.getUsername(), user.getFirstName(),
                user.getLastName(), subscribed, user.getAvatar());
    }

    public UserDto toDto(User user, User viewer) {
        boolean subscribed = viewer != null
                && followRepository.existsByUserIdAndAuthorId(viewer.getId(), user.getId());
        return toDto(user, subscribed);
    }

    public Set<Long> subscribedIds(User viewer, Collection<Long> authorIds) {
        if (viewer == null || authorIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(followRepository.findAuthorIds(viewer.getId(), authorIds));
    }

    public List<UserDto> toDtos(List<User> users, User viewer) {
        Set<Long> subscribed = subscribedIds(viewer, users.stream().map(User::getId).toList());
        return users.stream().map(u -> toDto(u, subscribed.contains(u.getId()))).toList();
    }
}
