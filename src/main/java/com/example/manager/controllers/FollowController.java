package com.example.manager.controllers;

import com.example.manager.dto.responses.Follow.UserFollowResponse;
import com.example.manager.services.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follows")
public class FollowController {
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{followerId}/follow/{followingId}")
    public ResponseEntity<String> followUser(@PathVariable String followerId, @PathVariable String followingId) {
        followService.followUser(followerId, followingId);
        return ResponseEntity.ok("Follow thành công!");
    }

    @DeleteMapping("/{followerId}/unfollow/{followingId}")
    public ResponseEntity<String> unfollowUser(@PathVariable String followerId, @PathVariable String followingId) {
        followService.unfollowUser(followerId, followingId);
        return ResponseEntity.ok("Unfollow thành công!");
    }


    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserFollowResponse>> getFollowers(@PathVariable String userId) {
        List<UserFollowResponse> followers = followService.getFollowers(userId);
        return ResponseEntity.ok(followers);
    }

    // API lấy danh sách người đang được theo dõi (following)
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserFollowResponse>> getFollowing(@PathVariable String userId) {
        List<UserFollowResponse> following = followService.getFollowing(userId);
        return ResponseEntity.ok(following);
    }
}
