package com.example.manager.services;

import com.example.manager.dto.requests.comment.CommentRequest;
import com.example.manager.dto.responses.comment.CommentItemResponse;
import com.example.manager.models.CommentEntity;
import com.example.manager.models.PostEntity;
import com.example.manager.models.UserEntity;
import com.example.manager.repositories.CommentRepository;
import com.example.manager.repositories.LikeRepository;
import com.example.manager.repositories.PostRepository;
import com.example.manager.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.xml.stream.events.Comment;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    public CommentItemResponse createComment(CommentRequest commentRequest){
        CommentEntity commentEntity = modelMapper.map(commentRequest,CommentEntity.class);
        PostEntity postEntity = postRepository.findById(commentRequest.getPostId())
                        .orElseThrow(() -> new RuntimeException("Post not found with id: " + commentRequest.getPostId()));
        UserEntity userEntity = userService.getUserById(commentRequest.getUserId());
        commentEntity.setPostId(postEntity.getPostId());
        commentEntity.setUser(userEntity);
        CommentEntity comment = commentRepository.save(commentEntity);

        CommentItemResponse commentItemResponse = modelMapper.map(commentEntity,CommentItemResponse.class);
        commentItemResponse.setUser(userService.getUserByIdResponse(commentRequest.getUserId()));
        commentItemResponse.setNumberLikeComment(0);
        commentItemResponse.setNumberReplyComment(0);
        return commentItemResponse;
    }


    public Page<CommentItemResponse> getAllCommentByPost(String postId,int page,int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentEntity> comments = commentRepository.getAllCommentByPost(postId,pageable);
        return comments.map(this::convertCommentResponse);
    }

    public CommentItemResponse convertCommentResponse(CommentEntity comment){
        CommentItemResponse commentItemResponse = modelMapper.map(comment,CommentItemResponse.class);
        commentItemResponse.setUser(userService.getUserByIdResponse(comment.getUser().getUserId()));
        commentItemResponse.setNumberLikeComment(likeRepository.countLikeComment(comment.getCommentId()));
        commentItemResponse.setNumberReplyComment(commentRepository.countReplyCommentByCommentParent(comment.getCommentId()));
        return commentItemResponse;
    }

    public Boolean deleteComment(String commentId){
        CommentEntity comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy commment"));
        commentRepository.delete(comment);
        return true;
    }


    public  Page<CommentItemResponse> getAllCommentReplyByCommentParent(String commentId,int page,int size){
        Pageable pageable = PageRequest.of(page,size);
        Page<CommentEntity> comments = commentRepository.getAllCommentReplyByCommentParent(commentId,pageable);
        return comments.map(this::convertCommentResponse);
    }



}
