package com.example.manager.services;

import com.example.manager.dto.requests.post.PostRequest;
import com.example.manager.dto.requests.post.UpdatePostRequest;
import com.example.manager.dto.responses.like.LikeItemResponse;
import com.example.manager.dto.responses.post.PostResponse;
import com.example.manager.dto.responses.user.ItemUserResponse;
import com.example.manager.models.*;
import com.example.manager.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final String BASE_UPLOAD_DIR = "./uploads/";

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ShareRepository shareRepository;


    @Autowired
    private ModelMapper modelMapper;

    //***
    // Thêm dữ liệu vào hàm ***** savePost  ************//
    //***
    public PostResponse savePostWithFiles(PostRequest postRequest, List<MultipartFile> files) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String typeFile = determineFileType(file);
            String fileUrl = uploadFile(file, typeFile);
            fileUrls.add(fileUrl);
        }

        return savePost(postRequest, fileUrls, files);
    }

    //Định nghĩa file
    private String determineFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        if (extension != null) {
            switch (extension.toLowerCase()) {
                case ".jpg":
                case ".jpeg":
                case ".png":
                case ".gif":
                case ".webp":
                    return "image";
                case ".mp4":
                case ".avi":
                case ".mov":
                    return "video";
                case ".mp3":
                case ".wav":
                    return "audio";
                default:
                    throw new IllegalArgumentException("Unsupported file type: " + extension);
            }
        }

        throw new IllegalArgumentException("File name is invalid: " + fileName);
    }

    /// FILE
    private String uploadFile(MultipartFile file, String typeFile) throws IOException {
        String uploadDir = getUploadDirectory(typeFile);
        if (uploadDir == null) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + (fileExtension != null ? fileExtension : "");
        Path filePath = Paths.get(uploadDir + newFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/" + typeFile.toLowerCase() + "s/" + newFileName;
    }

    private String getUploadDirectory(String typeFile) {
        return switch (typeFile.toLowerCase()) {
            case "image" -> BASE_UPLOAD_DIR + "images/";
            case "video" -> BASE_UPLOAD_DIR + "videos/";
            case "audio" -> BASE_UPLOAD_DIR + "audios/";
            default -> null;
        };
    }


    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.lastIndexOf('.') > 0) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return null;
    }


    ////////        ---- CREATE POST ---    ////////////////
    public PostResponse savePost(PostRequest postRequest, List<String> fileUrls, List<MultipartFile> files) {
        PostEntity postEntity = modelMapper.map(postRequest, PostEntity.class);
        UserEntity user = userRepository.findByUserId(postRequest.getUser())
                .orElseThrow(() -> new RuntimeException("User not found"));

        postEntity.setUser(user);
        PostEntity newPost = postRepository.save(postEntity);

        // Lưu các phương tiện
        for (int i = 0; i < fileUrls.size(); i++) {
            String fileUrl = fileUrls.get(i);
            String typeFile = determineFileType(files.get(i)); // Lấy kiểu tệp từ danh sách files

            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setPost(newPost);
            mediaEntity.setUrl(fileUrl);
            mediaEntity.setType(typeFile); // Gán giá trị type cho mediaEntity
            mediaRepository.save(mediaEntity);
        }

        ItemUserResponse itemUserResponse = modelMapper.map(user, ItemUserResponse.class);
        PostResponse postResponse = modelMapper.map(newPost, PostResponse.class);
        postResponse.setUser(itemUserResponse);
        postResponse.setMediaUrls(fileUrls);

        return postResponse;
    }



    /// --------------    DELETE   ----------------------///
    public Boolean deletePost(String postId) {
        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<MediaEntity> mediaEntities = mediaRepository.findByPost(postEntity);
        for (MediaEntity mediaEntity : mediaEntities) {
            String filePath = BASE_UPLOAD_DIR + mediaEntity.getUrl();
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            mediaRepository.delete(mediaEntity);
        }

        postRepository.delete(postEntity);
        return true;
    }


    // ----------------- Lấy tất cả bài viết ------------------------//
    public Page<PostResponse> getAllPost(int page, int size, String userName, Timestamp startDate, Timestamp endDate) {
        Pageable pageable = PageRequest.of(page, size);

        List<PostEntity> allPosts = postRepository.findAllPublicPostByFilter(userName,startDate,endDate);
        Collections.shuffle(allPosts); // Xáo trộn danh sách bài viết

        // Cắt danh sách theo phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min((start + size), allPosts.size());
        List<PostEntity> pagePosts = allPosts.subList(start, end);

        List<PostResponse> postResponses = pagePosts.stream().map(post -> {
            PostResponse response = modelMapper.map(post, PostResponse.class);

            List<String> mediaUrls = post.getMediaList().stream()
                    .map(MediaEntity::getUrl)
                    .collect(Collectors.toList());

            response.setMediaUrls(mediaUrls);
            response.setNumberLike(likeRepository.countLikePost(post.getPostId()));
            response.setNumberComment(commentRepository.countCommentByPost(post.getPostId()));
            response.setNumberShare(shareRepository.countShareByPostId(post.getPostId()));

            return response;
        }).collect(Collectors.toList());

        return new PageImpl<>(postResponses, pageable, allPosts.size());
    }


    // ----------- Get PostEntity ------------
    public PostEntity getItemPostEntity(String postId){
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }



    // ----------- GET ITEM POST RESPONSE  -------------
    public PostResponse getItemPost(String postId){
        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<String> mediaUrls = setUrlMediaResponse(postEntity);
        PostResponse postResponse = modelMapper.map(postEntity,PostResponse.class);
        int countLike = likeRepository.countLikePost(postId);
        int countComment = commentRepository.countCommentByPost(postId);
        int countShare = shareRepository.countShareByPostId(postId);
        postResponse.setMediaUrls(mediaUrls);
        postResponse.setNumberLike(countLike);
        postResponse.setNumberComment(countComment);
        postResponse.setNumberShare(countShare);

        if(postEntity.getIsPublicComment().equals(Boolean.FALSE)){
            postResponse.setNumberComment(0);
        }

        return postResponse;
    }


    // -------------- SET URL FOR POST RESPONSE -------------
    private List<String> setUrlMediaResponse(PostEntity post){
        List<MediaEntity> mediaEntities = mediaRepository.findByPost(post);
        return mediaEntities.stream().map(MediaEntity::getUrl).collect(Collectors.toList());
    }


    // -------------- GET ALL POST BY USER ID ------------
    public Page<PostResponse> getAllPostByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostEntity> postsEntityPage = postRepository.findAllByUserId(userId, pageable);

        return postsEntityPage.map(post -> getItemPost(post.getPostId()));
    }

    // -------------- GET ALL PRIVATE POST BY USER ID ------------
    public Page<PostResponse> getAllPrivatePostByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostEntity> postsEntityPage = postRepository.findAllPrivatePostByUserId(userId, pageable);

        return postsEntityPage.map(post -> getItemPost(post.getPostId()));
    }


    // -------------- GET ALL LIKE BY POST ID ------------
    public List<LikeItemResponse> getAllLikeByPost(String postId){
        List<LikeEntity> commentEntities = likeRepository.getAllLikeByPost(postId);
        return  commentEntities.stream().map(this::getItemLikeByPost).collect(Collectors.toList());

    }


    // -------------- CONVERT LIKE RESPONSE ------------
    public LikeItemResponse getItemLikeByPost(LikeEntity likeEntity){
        return modelMapper.map(likeEntity,LikeItemResponse.class);
    }


    // ------------- SET PUBLIC POST --------------
    public Boolean setPublicPost(String postId) {
        // Kiểm tra xem postId có tồn tại không
        Optional<PostEntity> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            PostEntity post = optionalPost.get();
            post.setIsPublicPost(true);
            postRepository.save(post);
            return true;
        }
        return false;
    }

    public Boolean setPrivatePost(String postId) {
        // Kiểm tra xem postId có tồn tại không
        Optional<PostEntity> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            PostEntity post = optionalPost.get();
            post.setIsPublicPost(false);
            postRepository.save(post);
            return true;
        }
        return false;
    }

    public Boolean setPublicComment(String postId) {
        // Kiểm tra xem postId có tồn tại không
        Optional<PostEntity> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            PostEntity post = optionalPost.get();
            post.setIsPublicComment(true);
            postRepository.save(post);
            return true;
        }
        return false;
    }

    public Boolean setPrivateComment(String postId) {
        // Kiểm tra xem postId có tồn tại không
        Optional<PostEntity> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            PostEntity post = optionalPost.get();
            post.setIsPublicComment(false);
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Transactional
    public Boolean updatePost(UpdatePostRequest updatePost,String postId){
        System.out.println("++++++++++==== "+ updatePost.isPublicPost());
        Optional<PostEntity> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            PostEntity post = optionalPost.get();
            post.setIsPublicPost(updatePost.isPublicPost());
            post.setIsPublicComment(updatePost.isPublicComment());
            postRepository.save(post);
            return true;
        }
        return false;
    }

}
