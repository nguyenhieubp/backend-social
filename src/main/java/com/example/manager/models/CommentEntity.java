package com.example.manager.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "comments")
public class CommentEntity {
    @Id
    @UuidGenerator
    private String commentId;

    @Column(name = "parent_comment_id",nullable = true)
    private String parentCommentId;

    @Column(name = "post_id", nullable = false)
    private String postId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(updatable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    private List<LikeEntity> likeList;

    // Getters and Setters
}
