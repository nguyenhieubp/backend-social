package com.example.manager.dto.responses.post;

import com.example.manager.dto.responses.user.ItemUserResponse;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class PostResponse {
    private String postId;
    private ItemUserResponse user;
    private String content;
    private Boolean isPublicPost;
    private Boolean isPublicComment;
    private List<String> mediaUrls;
    private Timestamp createdAt;
    private int numberLike;
    private int numberComment;
    private int numberShare;


}
