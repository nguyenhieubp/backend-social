package com.example.manager.repositories;


import com.example.manager.models.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;


@Repository
public interface PostRepository extends JpaRepository<PostEntity,String> {
    @Query("SELECT p FROM PostEntity p WHERE p.user.userId = :userId AND p.isPublicPost = TRUE")
    Page<PostEntity> findAllByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.user.userId = :userId AND p.isPublicPost = FALSE")
    Page<PostEntity> findAllPrivatePostByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p " +
            "WHERE p.isPublicPost = TRUE " +
            "AND (:username IS NULL OR LOWER(p.user.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "AND (:startDate IS NULL OR p.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR p.createdAt <= :endDate)")
    List<PostEntity> findAllPublicPostByFilter(
            @Param("username") String username,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate
    );

s

    @Query(value = """
        WITH daily_counts AS (
            SELECT 
                DATE(created_at) as date,
                COUNT(*) as new_posts
            FROM posts
            WHERE created_at BETWEEN ?1 AND ?2
            GROUP BY DATE(created_at)
        )
        SELECT 
            date,
            SUM(new_posts) OVER (ORDER BY date) as total_posts,
            new_posts
        FROM daily_counts
        ORDER BY date
    """, nativeQuery = true)
    List<Object[]> countPostsByDate(Timestamp startDate, Timestamp endDate);

}
