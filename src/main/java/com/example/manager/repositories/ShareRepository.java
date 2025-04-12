package com.example.manager.repositories;

import com.example.manager.models.ShareEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShareRepository  extends JpaRepository<ShareEntity,String> {
    @Query("SELECT share FROM ShareEntity share WHERE share.user.userId =:userId")
    Page<ShareEntity> getAllShareByUserId(String userId, Pageable pageable);

    @Query("SELECT COUNT(share) FROM ShareEntity share WHERE share.post.postId =:postId")
    Integer countShareByPostId(String postId);
}
