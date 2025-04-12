package com.example.manager.services;

import com.example.manager.dto.requests.user.ChangeName;
import com.example.manager.dto.requests.user.UpdateProfileRequest;
import com.example.manager.dto.requests.user.UserRequest;
import com.example.manager.dto.responses.role.RoleResponse;
import com.example.manager.dto.responses.user.ItemUserResponse;
import com.example.manager.dto.responses.user.ProfileUserResponse;
import com.example.manager.dto.responses.user.UserFriend;
import com.example.manager.dto.responses.user.UserRoleResponse;
import com.example.manager.models.RoleEntity;
import com.example.manager.models.UserEntity;
import com.example.manager.models.UserRoleEntity;
import com.example.manager.repositories.MediaRepository;
import com.example.manager.repositories.RoleRepository;
import com.example.manager.repositories.UserRepository;
import com.example.manager.repositories.UserRoleRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private MediaRepository mediaRepository;

    private static final String BASE_UPLOAD_DIR = "./uploads/";


    public Page<UserFriend> getUsers(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> usersPage = usersRepository.searchByName(name, pageable);
        return usersPage.map(user -> modelMapper.map(user, UserFriend.class));
    }


    public ProfileUserResponse profile (String userId){
        UserEntity user = usersRepository.profileUser(userId);
        Integer numberPost = Math.toIntExact(usersRepository.countUserPosts(userId));

        ProfileUserResponse response =  modelMapper.map(user,ProfileUserResponse.class);
        response.setNumberPost(numberPost);
        return response;
    };

    // Đăng ký user mới
    public void registerUser(UserRequest userRequest) {
        if (usersRepository.existsByEmail(userRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        UserEntity user = modelMapper.map(userRequest, UserEntity.class);
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Mã hóa mật khẩu

        UserEntity newUser = usersRepository.save(user);

        RoleEntity roleUser = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setRole(roleUser);
        userRole.setUser(newUser);

        userRoleRepository.save(userRole);
    }

    // Lấy danh sách UserRoleResponse
    public List<UserRoleResponse> getAllUserRoles() {
        return usersRepository.findAll()
                .stream()
                .map(this::convertToUserRoleResponse)
                .collect(Collectors.toList());
    }

    // Thay đổi tên người dùng
    public UserRoleResponse changeName(String userId, ChangeName name) {
        UserEntity user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setUsername(name.getUsername());
        usersRepository.save(user);

        UserRoleResponse userRoleResponse = this.convertToUserRoleResponse(user);
        userRoleResponse.setUserId(user.getUserId());

        return userRoleResponse;
    }

    // Tìm UserEntity theo ID
    public UserEntity getUserById(String userId) {
        return usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    // Tìm UserEntity theo ID (phản hồi dạng DTO)
    public ItemUserResponse getUserByIdResponse(String userId) {
        UserEntity userEntity = getUserById(userId);
        return modelMapper.map(userEntity, ItemUserResponse.class);
    }

    // Chuyển đổi UserEntity thành UserRoleResponse
    private UserRoleResponse convertToUserRoleResponse(UserEntity user) {
        UserRoleResponse response = new UserRoleResponse();
        response.setUsername(user.getUsername());

        List<RoleResponse> roleResponses = user.getUserRoles().stream()
                .map(userRole -> {
                    RoleResponse roleResponse = new RoleResponse();
                    roleResponse.setRoleId(userRole.getRole().getRoleId());
                    roleResponse.setRoleName(userRole.getRole().getRoleName());
                    return roleResponse;
                })
                .collect(Collectors.toList());

        response.setRoles(roleResponses);
        return response;
    }


    // Cập nhật ảnh đại diện
    public void changeProfilePicture(String userId, MultipartFile file) throws IOException {
        // Tìm kiếm người dùng
        UserEntity user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Xóa ảnh đại diện cũ (nếu có)
        if (user.getProfilePicture() != null) {
            String oldProfilePath = BASE_UPLOAD_DIR + user.getProfilePicture();
            File oldFile = new File(oldProfilePath);
            if (oldFile.exists() && !oldFile.delete()) {
                throw new IOException("Failed to delete old profile picture: " + oldProfilePath);
            }
        }

        // Kiểm tra định dạng file
        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (!isImageFile(fileExtension)) {
            throw new IllegalArgumentException("Unsupported file type. Only .jpg, .jpeg, .png, .gif are allowed.");
        }

        // Tạo thư mục lưu trữ nếu chưa tồn tại
        String uploadDir = BASE_UPLOAD_DIR + "images/";
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create upload directory.");
        }

        // Đặt tên mới cho file và lưu trữ
        String newFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = Paths.get(uploadDir, newFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Cập nhật đường dẫn ảnh đại diện cho người dùng
        String relativePath = "/images/" + newFileName;
        user.setProfilePicture(relativePath);
        usersRepository.save(user);
    }

    // Kiểm tra định dạng file ảnh
    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        String lowerCaseExt = extension.toLowerCase();
        return List.of(".jpg", ".jpeg", ".png", ".gif").contains(lowerCaseExt);
    }

    // Lấy đuôi file từ tên file
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.lastIndexOf('.') > 0) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return null;
    }

    public Boolean setShowShare(String userId){
        UserEntity user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found "+userId));
        user.setIsShowShare(true);
        usersRepository.save(user);
        return true;
    }


    public Boolean setOffShowShare(String userId){
        UserEntity user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found "+userId));
        user.setIsShowShare(false);
        usersRepository.save(user);
        return true;
    }

    public Boolean allowCheckShare(String userId){
        UserEntity user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found "+userId));
        return user.getIsShowShare();

    }

    public ProfileUserResponse updateProfile(String userId, UpdateProfileRequest request) throws IOException {
        UserEntity user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Cập nhật tên và bio nếu có
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        // Cập nhật ảnh đại diện nếu có file được gửi lên
        if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
            MultipartFile file = request.getProfilePicture();

            // Xóa ảnh đại diện cũ (nếu có)
            if (user.getProfilePicture() != null) {
                String oldProfilePath = BASE_UPLOAD_DIR + user.getProfilePicture();
                File oldFile = new File(oldProfilePath);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            // Kiểm tra định dạng file
            String fileExtension = getFileExtension(file.getOriginalFilename());
            if (!isImageFile(fileExtension)) {
                throw new IllegalArgumentException("Unsupported file type. Only .jpg, .jpeg, .png, .gif are allowed.");
            }

            // Lưu file mới
            String uploadDir = BASE_UPLOAD_DIR + "images/";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Cập nhật đường dẫn ảnh đại diện
            String relativePath = "/images/" + newFileName;
            user.setProfilePicture(relativePath);
        }

        // Lưu lại vào DB
        usersRepository.save(user);

        return modelMapper.map(user, ProfileUserResponse.class);
    }

}
