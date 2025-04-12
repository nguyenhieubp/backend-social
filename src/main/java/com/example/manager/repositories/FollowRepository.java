package com.example.manager.repositories;

import com.example.manager.models.FollowEntity;
import com.example.manager.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, String> {
    boolean existsByFollowerAndFollowing(UserEntity follower, UserEntity following);

    Optional<FollowEntity> findByFollowerAndFollowing(UserEntity follower, UserEntity following);

    List<FollowEntity> findByFollowing(UserEntity following);

    List<FollowEntity> findByFollower(UserEntity follower);
}

