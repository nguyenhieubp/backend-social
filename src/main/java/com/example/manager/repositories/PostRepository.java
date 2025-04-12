package com.example.manager.repositories;


import com.example.manager.models.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PostRepository extends JpaRepository<PostEntity,String> {
    @Query("SELECT p FROM PostEntity p WHERE p.user.userId = :userId AND p.isPublicPost = TRUE")
    Page<PostEntity> findAllByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.user.userId = :userId AND p.isPublicPost = FALSE")
    Page<PostEntity> findAllPrivatePostByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.isPublicPost = TRUE")
    List<PostEntity> findAllPublicPosts();


}
