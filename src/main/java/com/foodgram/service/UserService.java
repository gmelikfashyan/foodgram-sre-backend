package com.foodgram.service;

import com.foodgram.domain.AuthToken;
import com.foodgram.domain.User;
import com.foodgram.repository.AuthTokenRepository;
import com.foodgram.repository.UserRepository;
import com.foodgram.web.dto.SetPasswordRequest;
import com.foodgram.web.dto.UserCreateRequest;
import com.foodgram.web.error.ApiException;
import com.foodgram.web.error.FieldErrors;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.foodgram.web.dto.TokenLoginRequest;

@Service
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("^[\\w.@+-]+$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorage imageStorage;

    public UserService(UserRepository userRepository, AuthTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, ImageStorage imageStorage) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public Page<User> list(int page, int limit) {
        return userRepository.findAll(
                PageRequest.of(page - 1, limit, Sort.by("lastName", "firstName", "id")));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(ApiException::notFound);
    }

    @Transactional
    public User register(UserCreateRequest r) {
        FieldErrors e = new FieldErrors();

        String email = r.email() == null ? null : r.email().trim();
        if (isBlank(email)) {
            e.add("email", FieldErrors.REQUIRED);
        } else if (email.length() > 254) {
            e.add("email", "Убедитесь, что это значение содержит не более 254 символов.");
        } else if (!EMAIL.matcher(email).matches()) {
            e.add("email", "Введите правильный адрес электронной почты.");
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            e.add("email", "Пользователь с таким email уже существует.");
        }

        if (isBlank(r.username())) {
            e.add("username", FieldErrors.REQUIRED);
        } else if (r.username().length() > 150) {
            e.add("username", "Убедитесь, что это значение содержит не более 150 символов.");
        } else if (!USERNAME.matcher(r.username()).matches()) {
            e.add("username", "Username должен содержать только латинские буквы, цифры, знаки @ + - .");
        } else if (userRepository.existsByUsername(r.username())) {
            e.add("username", "Пользователь с таким username уже существует.");
        }

        requireName(e, "first_name", r.firstName());
        requireName(e, "last_name", r.lastName());

        if (isBlank(r.password())) {
            e.add("password", FieldErrors.REQUIRED);
        } else {
            validatePassword(e, "password", r.password(), r.username(), email);
        }
        e.throwIfAny();

        User user = new User(email, r.username(), r.firstName(), r.lastName(),
                passwordEncoder.encode(r.password()));
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw FieldErrors.single("email", "Пользователь с таким email или username уже существует.");
        }
    }

    /** Выдаёт токен (один на пользователя, как в DRF authtoken). */
    @Transactional
    public String login(TokenLoginRequest r) {
        FieldErrors e = new FieldErrors();
        if (isBlank(r.email())) {
            e.add("email", FieldErrors.REQUIRED);
        }
        if (isBlank(r.password())) {
            e.add("password", FieldErrors.REQUIRED);
        }
        e.throwIfAny();

        User user = userRepository.findByEmailIgnoreCase(r.email().trim())
                .filter(u -> passwordEncoder.matches(r.password(), u.getPassword()))
                .orElseThrow(() -> FieldErrors.single("non_field_errors",
                        "Невозможно войти с предоставленными учетными данными."));

        return tokenRepository.findByUserId(user.getId())
                .map(AuthToken::getKey)
                .orElseGet(() -> tokenRepository.save(
                        new AuthToken(generateKey(), user)).getKey());
    }

    @Transactional
    public void logout(String tokenKey) {
        if (tokenKey != null) {
            tokenRepository.deleteById(tokenKey);
        }
    }

    @Transactional
    public void setPassword(User viewer, SetPasswordRequest r) {
        FieldErrors e = new FieldErrors();
        User user = userRepository.findById(viewer.getId()).orElseThrow(ApiException::notFound);

        if (isBlank(r.currentPassword())) {
            e.add("current_password", FieldErrors.REQUIRED);
        } else if (!passwordEncoder.matches(r.currentPassword(), user.getPassword())) {
            e.add("current_password", "Неверный текущий пароль");
        }
        if (isBlank(r.newPassword())) {
            e.add("new_password", FieldErrors.REQUIRED);
        } else {
            validatePassword(e, "new_password", r.newPassword(), user.getUsername(), user.getEmail());
        }
        e.throwIfAny();

        user.setPassword(passwordEncoder.encode(r.newPassword()));
    }

    @Transactional
    public String setAvatar(User viewer, String data) {
        if (isBlank(data)) {
            throw FieldErrors.single("avatar", FieldErrors.REQUIRED);
        }
        ImageStorage.DecodedImage image;
        try {
            image = imageStorage.decode(data);
        } catch (IllegalArgumentException ex) {
            throw FieldErrors.single("avatar", ex.getMessage());
        }
        User user = userRepository.findById(viewer.getId()).orElseThrow(ApiException::notFound);
        String old = user.getAvatar();
        user.setAvatar(imageStorage.save("users/images", image));
        imageStorage.delete(old);
        return user.getAvatar();
    }

    @Transactional
    public void deleteAvatar(User viewer) {
        User user = userRepository.findById(viewer.getId()).orElseThrow(ApiException::notFound);
        if (user.getAvatar() == null) {
            throw ApiException.badRequest("У пользователя нет аватара.");
        }
        imageStorage.delete(user.getAvatar());
        user.setAvatar(null);
    }

    private static void requireName(FieldErrors e, String field, String value) {
        if (isBlank(value)) {
            e.add(field, FieldErrors.REQUIRED);
        } else if (value.length() > 150) {
            e.add(field, "Убедитесь, что это значение содержит не более 150 символов.");
        }
    }

    private static void validatePassword(FieldErrors e, String field, String password,
                                         String username, String email) {
        if (password.length() < 8) {
            e.add(field, "Введённый пароль слишком короткий. Он должен содержать как минимум 8 символов.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            e.add(field, "Введённый пароль слишком длинный (не более 72 байт).");
        }
        if (password.chars().allMatch(Character::isDigit)) {
            e.add(field, "Введённый пароль состоит только из цифр.");
        }
        if (password.equalsIgnoreCase(username) || password.equalsIgnoreCase(email)) {
            e.add(field, "Введённый пароль слишком похож на имя пользователя или email.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String generateKey() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
