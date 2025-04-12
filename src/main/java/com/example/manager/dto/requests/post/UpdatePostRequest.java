package com.example.manager.dto.requests.post;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class UpdatePostRequest {
    @JsonProperty("isPublicPost")
    private boolean isPublicPost;

    @JsonProperty("isPublicComment")
    private boolean isPublicComment;
}
