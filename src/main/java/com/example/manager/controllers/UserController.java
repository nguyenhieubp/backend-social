package com.example.manager.controllers;

import com.example.manager.dto.requests.user.ChangeName;
import com.example.manager.dto.requests.user.UpdateProfileRequest;
import com.example.manager.dto.requests.user.UserRequest;
import com.example.manager.dto.responses.common.ApiResponse;
import com.example.manager.dto.responses.post.PostResponse;
import com.example.manager.dto.responses.user.ProfileUserResponse;
import com.example.manager.dto.responses.user.UserFriend;
import com.example.manager.dto.responses.user.UserRoleResponse;
import com.example.manager.models.RoleEntity;
import com.example.manager.models.UserEntity;
import com.example.manager.models.UserRoleEntity;
import com.example.manager.repositories.RoleRepository;
import com.example.manager.repositories.UserRepository;
import com.example.manager.repositories.UserRoleRepository;
import com.example.manager.services.AuthenticationService;
import com.example.manager.services.PostService;
import com.example.manager.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final AuthenticationService authenticationService;

    @Autowired
    private UserService userService;


    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${jwt.signerKey}")
    private String signerKey;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserFriend> listShare = userService.getUsers(name,page-1,size);
        return ResponseEntity.ok(new ApiResponse<>(200, "OK", listShare));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<?>> profile(@PathVariable String userId){
        ApiResponse<ProfileUserResponse> apiResponse = new ApiResponse<>(200,"success",userService.profile(userId));
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody UserRequest user) {
        userService.registerUser(user); // Phương thức để đăng ký người dùng
        ApiResponse<String> apiResponse = new ApiResponse<>(201,"success","User registered successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/check/{userId}/allowCheckShare")
    public ResponseEntity<ApiResponse<?>> allowCheckShare(@PathVariable("userId") String userId){
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(201,"success",userService.allowCheckShare(userId));
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/update/{userId}")
    public ResponseEntity<ApiResponse<UserRoleResponse>> changeName(@PathVariable String userId, @Valid @RequestBody ChangeName name){
        ApiResponse<UserRoleResponse> apiResponse = new ApiResponse<>(201,"success",userService.changeName(userId,name));
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<?> getAllPostByUserId(@PathVariable("userId") String userId,@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size){
        ApiResponse<Page<PostResponse>> apiResponse = new ApiResponse<>(200,"success",postService.getAllPostByUserId(userId,page-1,size));
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{userId}/private/posts")
    public ResponseEntity<?> getAllPrivatePostByUserId(@PathVariable("userId") String userId,@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size){
        ApiResponse<Page<PostResponse>> apiResponse = new ApiResponse<>(200,"success",postService.getAllPrivatePostByUserId(userId,page-1,size));
        return ResponseEntity.ok(apiResponse);
    }


    @PatchMapping("/update/profilePicture/{userId}")
    public ResponseEntity<ApiResponse<?>> changeProfilePicture(
            @PathVariable String userId,
            @RequestPart("file") MultipartFile file) throws IOException {
        userService.changeProfilePicture(userId, file);
        ApiResponse<?> apiResponse = new ApiResponse<>(201, "success", "Cập Nhật Thành Công");
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/update/isShowShare/true/{userId}")
    public ResponseEntity<ApiResponse<?>> setShowShare(@PathVariable("userId") String userId){
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(201, "set show success",  userService.setShowShare(userId));
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/update/isShowShare/false/{userId}")
    public ResponseEntity<ApiResponse<?>> setOffShowShare(@PathVariable("userId") String userId){
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(201, "set not show success",  userService.setOffShowShare(userId));
        return ResponseEntity.ok(apiResponse);
    }


    @PutMapping("/update/profile/{userId}")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @PathVariable String userId,
            @ModelAttribute UpdateProfileRequest request) throws IOException {

        ProfileUserResponse updatedUser = userService.updateProfile(userId, request);
        ApiResponse<ProfileUserResponse> apiResponse = new ApiResponse<>(201, "success", updatedUser);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/growth")
    public ResponseEntity<ApiResponse<?>> getUserGrowth(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            // Nếu không có ngày bắt đầu, mặc định lấy 30 ngày gần nhất
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30).toString();
            }
            if (endDate == null) {
                endDate = LocalDate.now().toString();
            }

            // Query để lấy số lượng user theo ngày
            List<Object[]> results = userRepository.countUsersByDate(
                    Timestamp.valueOf(startDate + " 00:00:00"),
                    Timestamp.valueOf(endDate + " 23:59:59")
            );

            // Format kết quả
            List<Map<String, Object>> data = results.stream()
                    .map(result -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("date", result[0]); // Ngày
                        item.put("count", result[1]); // Tổng số user đến ngày đó
                        item.put("newUsers", result[2]); // Số user mới trong ngày
                        return item;
                    })
                    .collect(Collectors.toList());

            ApiResponse<List<Map<String, Object>>> apiResponse =
                    new ApiResponse<>(200, "success", data);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<?> apiResponse =
                    new ApiResponse<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }


    @GetMapping("/count")
    public ResponseEntity<ApiResponse<?>> getTotalUsers() {
        try {
            long totalUsers = userRepository.count();

            Map<String, Object> data = new HashMap<>();
            data.put("totalUsers", totalUsers);

            ApiResponse<Map<String, Object>> apiResponse =
                    new ApiResponse<>(200, "success", data);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<?> apiResponse =
                    new ApiResponse<>(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @PostMapping("/setAdmin/{userId}")
    public ResponseEntity<ApiResponse<?>> setAdmin(@PathVariable("userId") String userId) {
        Optional<UserEntity> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        RoleEntity roleAdmin = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(userOpt.get());
        userRole.setRole(roleAdmin);

        userRoleRepository.save(userRole);   // Lưu vào DB nếu cần

        return ResponseEntity.ok(new ApiResponse<>(200,"SUCCESS",null));
    }


}
