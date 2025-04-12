package com.example.manager.controllers;

import com.example.manager.dto.requests.PostReport.PostDTO;
import com.example.manager.dto.requests.PostReport.PostReportDTO;
import com.example.manager.dto.requests.PostReport.UserDTO;
import com.example.manager.dto.requests.post.HandleReportRequest;
import com.example.manager.dto.requests.post.PostRequest;
import com.example.manager.dto.requests.post.ReportPostRequest;
import com.example.manager.dto.requests.post.UpdatePostRequest;
import com.example.manager.dto.responses.common.ApiResponse;
import com.example.manager.dto.responses.like.LikeItemResponse;
import com.example.manager.dto.responses.post.PostResponse;
import com.example.manager.models.PostEntity;
import com.example.manager.models.PostReportEntity;
import com.example.manager.models.UserEntity;
import com.example.manager.repositories.PostReportRepository;
import com.example.manager.repositories.PostRepository;
import com.example.manager.repositories.UserRepository;
import com.example.manager.services.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostReportRepository postReportRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestPart("files") List<MultipartFile> files,
            @Valid @RequestPart("post") PostRequest post
    ) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("Files must be provided");
        }

        try {
            PostResponse postResponse = postService.savePostWithFiles(post, files);
            ApiResponse<PostResponse> response = new ApiResponse<>(201, "Create success", postResponse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload files");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPost(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostResponse> postResponses = postService.getAllPost(page-1,size);
        return ResponseEntity.ok(new ApiResponse<>(200, "Fetch success", postResponses));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable String postId) {
        try {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            // Xóa tất cả các report liên quan
            post.removeAllReports();

            // Xóa post
            postRepository.delete(post);
            return ResponseEntity.ok(new ApiResponse<>(200, "Delete success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(new ApiResponse<>(404, e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(500, "Failed to delete post", null));
        }
    }

    @GetMapping("/item/{postId}")
    public ResponseEntity<?> getItemPost(@PathVariable("postId") String postId){
        PostResponse postResponses = postService.getItemPost(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Fetch success", postResponses));
    }

    @GetMapping("/like-all/{postId}")
    public ResponseEntity<?> getAllLike(@PathVariable("postId") String postId){
        List<LikeItemResponse> likeItemResponses = postService.getAllLikeByPost(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Fetch success", likeItemResponses));
    }

    @PutMapping("/update/{postId}")
    public ResponseEntity<?> updatePost(@RequestBody UpdatePostRequest updatePost,@PathVariable("postId") String postId) {
        Boolean isSuccess = postService.updatePost(updatePost,postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Fetch success", isSuccess));
    }



    @PostMapping("/{postId}/report")
    public ResponseEntity<ApiResponse<?>> reportPost(
            @PathVariable String postId,
            @RequestBody ReportPostRequest request
    ) {
        try {
            // Kiểm tra bài viết tồn tại
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            // Kiểm tra người dùng tồn tại
            UserEntity user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Kiểm tra người dùng đã báo cáo bài viết này chưa
            if (postReportRepository.existsByPostAndReporter(post, user)) {
                throw new RuntimeException("You have already reported this post");
            }

            // Tạo báo cáo mới
            PostReportEntity report = new PostReportEntity();
            report.setPost(post);
            report.setReporter(user);
            report.setReason(request.getReason());
            report.setDescription(request.getDescription());

            postReportRepository.save(report);

            ApiResponse<?> apiResponse =
                    new ApiResponse<>(201, "Report submitted successfully", null);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<?> apiResponse =
                    new ApiResponse<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    // API để admin xem danh sách báo cáo
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<?>> getPostReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<PostReportEntity> reports;
            if (status != null) {
                reports = postReportRepository.findReportsByStatus(status, pageable);
            } else {
                reports = postReportRepository.findAll(pageable);
            }

            // Chuyển đổi sang DTO
            Page<PostReportDTO> reportDTOs = reports.map(report -> {
                PostReportDTO dto = new PostReportDTO();
                dto.setReportId(report.getReportId());
                dto.setPost(convertPostToDTO(report.getPost()));
                dto.setReporter(convertUserToDTO(report.getReporter()));
                dto.setReason(report.getReason());
                dto.setDescription(report.getDescription());
                dto.setStatus(report.getStatus());
                dto.setCreatedAt(report.getCreatedAt().toString());
                return dto;
            });

            ApiResponse<Page<PostReportDTO>> apiResponse =
                    new ApiResponse<>(200, "success", reportDTOs);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<?> apiResponse =
                    new ApiResponse<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    // API để admin xử lý báo cáo
    @PutMapping("/reports/{reportId}/handle")
    public ResponseEntity<ApiResponse<?>> handleReport(
            @PathVariable String reportId,
            @RequestBody HandleReportRequest request
    ) {
        try {
            PostReportEntity report = postReportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

            report.setStatus(request.getStatus());
            report.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // Nếu báo cáo được chấp nhận, có thể thêm logic xử lý bài viết
            if ("APPROVED".equals(request.getStatus())) {
                // Ví dụ: ẩn bài viết
                report.getPost().setIsPublicPost(false);
                postRepository.save(report.getPost());
            }

            postReportRepository.save(report);

            ApiResponse<?> apiResponse =
                    new ApiResponse<>(200, "Report handled successfully", null);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<?> apiResponse =
                    new ApiResponse<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }


    private PostDTO convertPostToDTO(PostEntity entity) {
        PostDTO dto = new PostDTO();
        dto.setPostId(entity.getPostId());
        dto.setContent(entity.getContent());
        dto.setUser(convertUserToDTO(entity.getUser()));
        dto.setCreatedAt(entity.getCreatedAt().toString());
        dto.setUpdatedAt(entity.getUpdatedAt().toString());
        dto.setIsPublicPost(entity.getIsPublicPost());
        dto.setIsPublicComment(entity.getIsPublicComment());
        return dto;
    }

    // Phương thức chuyển đổi UserEntity sang UserDTO
    private UserDTO convertUserToDTO(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setProfilePicture(entity.getProfilePicture());
        return dto;
    }
}
