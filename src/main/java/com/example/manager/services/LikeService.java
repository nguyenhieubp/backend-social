package com.example.manager.services;

import com.example.manager.dto.requests.like.LikeChildComment;
import com.example.manager.dto.requests.like.LikeCommentRequest;
import com.example.manager.dto.requests.like.LikePostRequest;
import com.example.manager.models.LikeEntity;
import com.example.manager.repositories.CommentRepository;
import com.example.manager.repositories.LikeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    public LikeRepository likeRepository;

    @Autowired
    public CommentRepository commentRepository;

    @Autowired
    public PostService postService;

    @Autowired
    public UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    public Boolean likePost(LikePostRequest likeRequest){
        LikeEntity isLike = unLikePost(likeRequest);
        if(isLike != null){
            likeRepository.delete(isLike);
            return false;
        };
        LikeEntity like = modelMapper.map(likeRequest,LikeEntity.class);
        like.setPost(postService.getItemPostEntity(likeRequest.getPostId()));
        like.setUser(userService.getUserById(likeRequest.getUserId()));
        LikeEntity likeSave = likeRepository.save(like);
        return true;
    }


    public LikeEntity unLikePost(LikePostRequest likeRequest){
        return likeRepository.findByUserAndPost(likeRequest.getPostId(), likeRequest.getUserId());
    }


    public Boolean likeComment(LikeCommentRequest likeCommentRequest){
        LikeEntity like = likeRepository.findByUserAndComment(likeCommentRequest.getCommentId(),likeCommentRequest.getUserId());
        if(like != null){
            likeRepository.delete(like);
            return false;
        };
        LikeEntity  likeEntity =  modelMapper.map(likeCommentRequest,LikeEntity.class);
        likeEntity.setComment(commentRepository.getById(likeCommentRequest.getCommentId()));
        likeEntity.setUser(userService.getUserById(likeCommentRequest.getUserId()));
        likeRepository.save(likeEntity);
        return true;
    }


}
