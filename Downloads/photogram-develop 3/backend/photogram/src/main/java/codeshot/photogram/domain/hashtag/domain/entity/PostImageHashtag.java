package codeshot.photogram.domain.hashtag.domain.entity;

import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "post_hashtag")
@Getter @Setter
public class PostImageHashtag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private PostImage postImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    protected PostImageHashtag() {}

    // ✅ 정적 팩토리 메서드 추가
    public static PostImageHashtag create(PostImage postImage, Hashtag hashtag) {
        PostImageHashtag postImageHashtag = new PostImageHashtag();
        postImageHashtag.setPostImage(postImage);
        postImageHashtag.setHashtag(hashtag);
        return postImageHashtag;
    }
}
