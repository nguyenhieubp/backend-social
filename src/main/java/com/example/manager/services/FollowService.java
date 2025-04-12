package com.example.manager.services;

import com.example.manager.dto.responses.Follow.UserFollowResponse;
import com.example.manager.dto.responses.user.ItemUserResponse;
import com.example.manager.models.FollowEntity;
import com.example.manager.models.UserEntity;
import com.example.manager.repositories.FollowRepository;
import com.example.manager.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    // Follow một người dùng
    public void followUser(String followerId, String followingId) {
        UserEntity follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + followerId));
        UserEntity following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + followingId));

        // Kiểm tra nếu đã follow rồi
        boolean alreadyFollowing = followRepository.existsByFollowerAndFollowing(follower, following);
        if (alreadyFollowing) {
            throw new RuntimeException("Bạn đã follow người dùng này rồi!");
        }

        FollowEntity follow = new FollowEntity();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);
    }

    // Unfollow một người dùng
    public void unfollowUser(String followerId, String followingId) {
        UserEntity follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + followerId));
        UserEntity following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + followingId));

        FollowEntity follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new RuntimeException("Bạn chưa follow người dùng này!"));
        followRepository.delete(follow);
    }

    // Lấy danh sách followers
    public List<UserFollowResponse> getFollowers(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + userId));
        List<FollowEntity> followers = followRepository.findByFollowing(user);

        // Map dữ liệu từ FollowEntity sang UserFollowResponse
        return followers.stream()
                .map(follow -> {
                    UserEntity follower = follow.getFollower();
                    return new UserFollowResponse(
                            follower.getUserId(),
                            follower.getUsername(),
                            follower.getEmail(),
                            follower.getProfilePicture()
                    );
                })
                .collect(Collectors.toList());
    }

    // Lấy danh sách following
    public List<UserFollowResponse> getFollowing(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại: " + userId));
        List<FollowEntity> following = followRepository.findByFollower(user);

        // Map dữ liệu từ FollowEntity sang UserFollowResponse
        return following.stream()
                .map(follow -> {
                    UserEntity followingUser = follow.getFollowing();
                    return new UserFollowResponse(
                            followingUser.getUserId(),
                            followingUser.getUsername(),
                            followingUser.getEmail(),
                            followingUser.getProfilePicture()
                    );
                })
                .collect(Collectors.toList());
    }

}
