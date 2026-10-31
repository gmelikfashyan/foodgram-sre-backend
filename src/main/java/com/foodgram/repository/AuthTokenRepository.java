package com.foodgram.repository;

import com.foodgram.domain.AuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    @Query("select t from AuthToken t join fetch t.user where t.key = :key")
    Optional<AuthToken> findWithUserByKey(@Param("key") String key);

    Optional<AuthToken> findByUserId(Long userId);
}
