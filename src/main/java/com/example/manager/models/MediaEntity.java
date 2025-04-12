package com.example.manager.models;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
@Entity
@Data
@Table(name = "media")
public class MediaEntity {
    @Id
    @UuidGenerator
    private String mediaId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, length = 50)
    private String type; // image hoặc video

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;
}