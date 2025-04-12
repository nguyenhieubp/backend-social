package com.example.manager.controllers;

import com.example.manager.dto.requests.comment.CommentRequest;
import com.example.manager.dto.responses.comment.CommentItemResponse;
import com.example.manager.dto.responses.common.ApiResponse;
import com.example.manager.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;


    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CommentItemResponse>> createComment(@RequestBody CommentRequest commentRequest){
        CommentItemResponse commentItemResponse = commentService.createComment(commentRequest);
        return ResponseEntity.ok(new ApiResponse<>(200, "Đã comment bài viết", commentItemResponse));
    }

    @GetMapping("/{postId}/all")
    public ResponseEntity<ApiResponse<Page<CommentItemResponse>>> getAllCommentByPost(@PathVariable("postId") String postId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size){
        Page<CommentItemResponse> commentResponse = commentService.getAllCommentByPost(postId,page-1,size);
        return ResponseEntity.ok(new ApiResponse<>(200, "Các comment bài viết", commentResponse));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteComment(@PathVariable("commentId") String commentId){
        Boolean isSuccess = commentService.deleteComment(commentId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Xóa comment bài viết thành công", isSuccess));
    }

    @GetMapping("/{commentId}/getAllCommentReply")
    public ResponseEntity<ApiResponse<Page<CommentItemResponse>>> getAllCommentReplyByCommentParent(@PathVariable("commentId") String commentId,@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size){
        Page<CommentItemResponse> comments = commentService.getAllCommentReplyByCommentParent(commentId,page-1,size);
        return ResponseEntity.ok(new ApiResponse<>(200, "Danh sách reply comment của "+commentId, comments));
    }
}
