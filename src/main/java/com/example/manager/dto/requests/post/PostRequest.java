package com.example.manager.dto.requests.post;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PostRequest {
    @NotEmpty(message = "Cần có người dùng đăng bài")
    private String user;

    @NotEmpty(message = "Bài viết cần có nội dung")
    private String content;

    private Boolean isPublicPost;
    private Boolean isPublicComment;
}
