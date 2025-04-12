package com.example.manager.dto.requests.PostReport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private String postId;
    private String content;
    private UserDTO user;
    private String createdAt;
    private String updatedAt;
    private Boolean isPublicPost;
    private Boolean isPublicComment;
}
