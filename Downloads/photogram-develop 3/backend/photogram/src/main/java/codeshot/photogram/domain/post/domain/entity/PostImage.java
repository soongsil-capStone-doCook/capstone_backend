package codeshot.photogram.domain.post.domain.entity;

import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photo")
@Getter @Setter
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;

    @Column(name = "photo_url", length = 200)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToMany(mappedBy = "postImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImageHashtag> postImageHashtags = new ArrayList<>();
}
