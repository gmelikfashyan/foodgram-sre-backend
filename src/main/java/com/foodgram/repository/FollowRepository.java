package com.foodgram.repository;

import com.foodgram.domain.Follow;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByUserIdAndAuthorId(Long userId, Long authorId);

    @Modifying
    @Query("delete from Follow f where f.user.id = :userId and f.author.id = :authorId")
    int deleteByUserAndAuthor(@Param("userId") Long userId, @Param("authorId") Long authorId);

    @EntityGraph(attributePaths = {"author"})
    Page<Follow> findByUserId(Long userId, Pageable pageable);

    @Query("select f.author.id from Follow f where f.user.id = :userId and f.author.id in :authorIds")
    List<Long> findAuthorIds(@Param("userId") Long userId, @Param("authorIds") Collection<Long> authorIds);
}
