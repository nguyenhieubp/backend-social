package com.example.manager.repositories;

import com.example.manager.models.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LikeRepository extends JpaRepository<LikeEntity,String> {
    @Query("SELECT like  FROM LikeEntity like WHERE like.post.postId = :postId AND like.user.userId = :userId")
    LikeEntity findByUserAndPost(String postId,String userId);

    @Query("SELECT COUNT(like) FROM LikeEntity like WHERE like.post.postId = :postId")
    Integer countLikePost(String postId);

    @Query("SELECT like FROM LikeEntity like WHERE like.post.postId = :postId")
    List<LikeEntity> getAllLikeByPost(String postId);

    @Query("SELECT like FROM LikeEntity like WHERE like.comment.commentId = :commentId AND like.user.userId = :userId")
    LikeEntity findByUserAndComment(String commentId,String userId);

    @Query("SELECT COUNT(like) FROM LikeEntity like WHERE like.comment.commentId = :commentId")
    Integer countLikeComment(String commentId);


}
