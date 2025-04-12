package com.example.manager.repositories;

import com.example.manager.models.MediaEntity;
import com.example.manager.models.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<MediaEntity,String> {
    List<MediaEntity> findByPost(PostEntity postEntity);
}
