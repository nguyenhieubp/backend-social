package com.example.manager.repositories;

import com.example.manager.models.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity,String> {
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = :postId")
    Integer countCommentByPost(String postId);

    @Query("SELECT comment FROM CommentEntity comment WHERE comment.postId = :postId AND comment.parentCommentId IS NULL")
    Page<CommentEntity> getAllCommentByPost(String postId, Pageable pageable);

    @Query("SELECT comment FROM CommentEntity comment WHERE comment.parentCommentId = :commentId")
    Page<CommentEntity> getAllCommentReplyByCommentParent(String commentId,Pageable pageable);

    @Query("SELECT COUNT(comment) FROM CommentEntity comment WHERE comment.parentCommentId = :commentId")
    Integer countReplyCommentByCommentParent(String commentId);

}