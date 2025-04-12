package com.example.manager.dto.responses.comment;

import com.example.manager.dto.responses.user.ItemUserResponse;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class CommentItemResponse {
    private String commentId;
    private String content;
    private ItemUserResponse user;
    private Timestamp createdAt;
    private Integer numberReplyComment;
    private Integer numberLikeComment;

}
